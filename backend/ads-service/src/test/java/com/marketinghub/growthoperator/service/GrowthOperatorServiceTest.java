package com.marketinghub.growthoperator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDetailedEventDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsEvidenceDto;
import com.marketinghub.growthoperator.GrowthOperatorExecution;
import com.marketinghub.growthoperator.service.start.StartGrowthOperatorRequest;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.growthoperator.GrowthOperatorExecutionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: validar o contexto auditável entregue ao Operador de Crescimento. */
class GrowthOperatorServiceTest {

  /** Confirma que eventos detalhados de sessão entram no snapshot do ciclo. */
  @Test
  void shouldFreezeDetailedSessionEvidence() throws Exception {
    GrowthOperatorExecutionRepository repository = mock(GrowthOperatorExecutionRepository.class);
    CommercialPlanService planService = mock(CommercialPlanService.class);
    ExperimentFunnelService funnelService = mock(ExperimentFunnelService.class);
    Experiment experiment = new Experiment();
    experiment.setId(81L);
    CommercialPlan plan = CommercialPlan.builder().id(2L).experiment(experiment).build();
    when(planService.getPlan(2L)).thenReturn(plan);
    when(repository.findByCommercialPlanIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());
    when(repository.save(any(GrowthOperatorExecution.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var event =
        new ExperimentLandingAnalyticsDetailedEventDto(
            10L,
            "visitor-a1b2c3",
            "session-d4e5f6",
            "checkout_click",
            "offer",
            Instant.parse("2026-08-04T10:00:00Z"),
            Map.of("deviceType", "mobile"));
    when(funnelService.buildDetailedAnalyticsEvidence(81L, 2000))
        .thenReturn(
            new ExperimentLandingAnalyticsEvidenceDto(81L, 1, 1, false, null, List.of(event)));
    when(funnelService.buildDetailedPdeAnalyticsEvidence(81L))
        .thenReturn(Map.of("available", false));
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    GrowthOperatorService service =
        new GrowthOperatorService(repository, planService, funnelService, objectMapper);

    service.start(2L, new StartGrowthOperatorRequest(1, "Diagnosticar funil"));

    ArgumentCaptor<GrowthOperatorExecution> captor =
        ArgumentCaptor.forClass(GrowthOperatorExecution.class);
    verify(repository).save(captor.capture());
    var snapshot = objectMapper.readTree(captor.getValue().getEvidenceSnapshot());
    assertThat(snapshot.at("/experimentId").asLong()).isEqualTo(81L);
    assertThat(snapshot.at("/sessionIntelligence/landingAnalytics/includedEvents").asInt())
        .isEqualTo(1);
    assertThat(
            snapshot
                .at("/sessionIntelligence/landingAnalytics/detailedEvents/0/anonymousSessionId")
                .asText())
        .isEqualTo("session-d4e5f6");
  }
}
