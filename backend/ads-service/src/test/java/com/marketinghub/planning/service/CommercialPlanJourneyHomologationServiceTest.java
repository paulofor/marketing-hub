package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.agent.v1.LandingGenerationAgentExecutionService;
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
            contains("commercial-plan-homologation-2-"),
            contains("\"mediaSpendAuthorized\":false"));
  }
}
