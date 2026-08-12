package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
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

  /** Confirma que o experimento vinculado recebe uma homologação segregada e sem gasto. */
  @Test
  void requestsIsolatedJourneyHomologationForLinkedExperiment() {
    CommercialPlan plan =
        CommercialPlan.builder().id(2L).experiment(Experiment.builder().id(88L).build()).build();
    when(commercialPlanService.getPlan(2L)).thenReturn(plan);
    var service =
        new CommercialPlanJourneyHomologationService(
            commercialPlanService, executionService, new ObjectMapper());

    var result = service.request(2L);

    assertThat(result.planId()).isEqualTo(2L);
    assertThat(result.experimentId()).isEqualTo(88L);
    assertThat(result.status()).isEqualTo("INICIADO");
    verify(executionService)
        .enqueue(
            eq(88L),
            contains("commercial-plan-homologation-2-"),
            contains("\"mediaSpendAuthorized\":false"));
  }
}
