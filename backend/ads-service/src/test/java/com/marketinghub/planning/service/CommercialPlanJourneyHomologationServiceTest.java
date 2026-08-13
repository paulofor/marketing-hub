package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.agent.v1.LandingGenerationAgentExecutionService;
import com.marketinghub.planning.CommercialPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: proteger o enfileiramento auditável da homologação do plano comercial. */
@ExtendWith(MockitoExtension.class)
class CommercialPlanJourneyHomologationServiceTest {
  @Mock private CommercialPlanService commercialPlanService;
  @Mock private LandingGenerationAgentExecutionService executionService;

  /** Confirma que o experimento escolhido recebe uma homologação segregada e sem gasto. */
  @Test
  void requestsIsolatedJourneyHomologationForLinkedExperiment() {
    when(commercialPlanService.getPlan(2L))
        .thenReturn(
            CommercialPlan.builder()
                .id(2L)
                .successCriteria("Landing com quatro exemplos finais e três criativos aprovados")
                .stopCriteria("Parar antes de gasto ou publicação externa")
                .currentBlocker("Prova visual incompleta")
                .rootCause("Contrato sem critério observável")
                .nextAction("Dédalo itera na sandbox e Têmis revisa")
                .build());
    var service =
        new CommercialPlanJourneyHomologationService(
            commercialPlanService, executionService, new ObjectMapper());

    var result = service.request(2L, 88L);

    assertThat(result.planId()).isEqualTo(2L);
    assertThat(result.experimentId()).isEqualTo(88L);
    assertThat(result.status()).isEqualTo("INICIADO");
    verify(commercialPlanService).requireExperiment(2L, 88L);
    verify(executionService)
        .enqueue(
            eq(88L),
            argThat(cycleId -> cycleId.startsWith("cph-2-") && cycleId.length() <= 36),
            argThat(
                brief ->
                    brief.contains("\"mediaSpendAuthorized\":false")
                        && brief.contains("\"recoveryPolicy\":\"RETRY_ON_EXECUTOR_DEPLOY\"")
                        && brief.contains("quatro exemplos finais")
                        && brief.contains("Dédalo itera na sandbox")));
  }
}
