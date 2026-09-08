package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVersion;
import com.marketinghub.repository.jpa.planning.CommercialPlanVersionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar o congelamento versionado do contexto comercial dos agentes. */
@ExtendWith(MockitoExtension.class)
class CommercialPlanVersionServiceTest {
  @Mock private CommercialPlanVersionRepository repository;

  /** Congela seleção, causa e metas comerciais sem reescrever a versão após mudanças do plano. */
  @Test
  void snapshotFreezesSalesAndProfitContext() throws Exception {
    when(repository.findTopByPlanIdOrderByVersionNumberDesc(7L)).thenReturn(Optional.empty());
    when(repository.save(any(CommercialPlanVersion.class)))
        .thenAnswer(
            invocation -> {
              CommercialPlanVersion version = invocation.getArgument(0);
              version.setId(11L);
              return version;
            });
    CommercialPlanVersionService service =
        new CommercialPlanVersionService(
            repository,
            new ObjectMapper().findAndRegisterModules(),
            Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneOffset.UTC));
    CommercialPlan plan =
        CommercialPlan.builder()
            .id(7L)
            .name("MUSA v7")
            .commercialObjective("Vender com lucro")
            .mainOffer("Método MUSA")
            .maxBudget(BigDecimal.valueOf(500))
            .targetRevenue(BigDecimal.valueOf(1000))
            .operationalRevenueTarget(BigDecimal.valueOf(335))
            .rootCause("Seleção anterior sobrescrita pelo polling")
            .experiment(Experiment.builder().id(91L).build())
            .experimentsToCreate(2)
            .experimentsToPublish(1)
            .productsToValidate(1)
            .productTypesToExplore(1)
            .approachesToTest(2)
            .customerConversationsTarget(2)
            .build();
    plan.getExperiments().add(Experiment.builder().id(90L).build());
    plan.getExperiments().add(plan.getExperiment());

    var version = service.snapshot(plan, "USER", "Definição inicial");

    assertThat(version.versionNumber()).isEqualTo(1);
    assertThat(version.snapshotJson())
        .contains(
            "Vender com lucro",
            "maxBudgetBrl",
            "\"experimentsToCreate\":2",
            "\"customerConversationsTarget\":2");
    assertThat(version.snapshotJson()).doesNotContain("hibernateLazyInitializer", "debug");
    plan.setExperiment(Experiment.builder().id(90L).build());
    var frozen = new ObjectMapper().readTree(version.snapshotJson());
    assertThat(frozen.path("experimentId").asLong()).isEqualTo(91L);
    assertThat(frozen.path("experimentIds").toString()).isEqualTo("[90,91]");
    assertThat(frozen.path("operationalRevenueTargetBrl").decimalValue())
        .isEqualByComparingTo("335");
    assertThat(frozen.path("rootCause").asText()).contains("polling");
  }
}
