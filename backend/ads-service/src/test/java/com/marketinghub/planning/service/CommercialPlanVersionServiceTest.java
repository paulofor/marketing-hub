package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
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

  /** Cria uma versão nova sem contaminar o snapshot com detalhes técnicos da entidade. */
  @Test
  void snapshotFreezesSalesAndProfitContext() {
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
            .build();

    var version = service.snapshot(plan, "USER", "Definição inicial");

    assertThat(version.versionNumber()).isEqualTo(1);
    assertThat(version.snapshotJson()).contains("Vender com lucro", "maxBudgetBrl");
    assertThat(version.snapshotJson()).doesNotContain("hibernateLazyInitializer", "debug");
  }
}
