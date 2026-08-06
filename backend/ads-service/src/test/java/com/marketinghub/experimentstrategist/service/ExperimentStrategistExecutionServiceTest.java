package com.marketinghub.experimentstrategist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar fila e congelamento auditavel da pesquisa estrategica. */
class ExperimentStrategistExecutionServiceTest {
  /** Cria uma pendencia somente leitura com a evidencia congelada. */
  @Test
  void startsPendingResearchWithFrozenEvidence() {
    ExperimentStrategistExecutionRepository repository =
        mock(ExperimentStrategistExecutionRepository.class);
    CommercialPlanService plans = mock(CommercialPlanService.class);
    ExperimentStrategistContextService contexts = mock(ExperimentStrategistContextService.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(7L);
    when(plans.getPlan(7L)).thenReturn(plan);
    when(contexts.researchContext(7L)).thenReturn(Map.of("bottleneck", "CHECKOUT"));
    when(repository.save(any()))
        .thenAnswer(
            invocation -> {
              ExperimentStrategistExecution value = invocation.getArgument(0);
              value.setId(12L);
              value.setCreatedAt(Instant.now());
              return value;
            });
    ExperimentStrategistExecutionService service =
        new ExperimentStrategistExecutionService(repository, plans, contexts, new ObjectMapper());

    var result =
        service.start(
            7L, new ExperimentStrategistExecutionService.StartRequest("Qual oferta testar?"));

    assertThat(result.status()).isEqualTo(ExperimentStrategistExecutionStatus.PENDING);
    assertThat(result.authorityMode()).isEqualTo("READ_ONLY_RESEARCH");
    assertThat(result.evidenceSnapshot()).contains("CHECKOUT");
    verify(repository).save(any(ExperimentStrategistExecution.class));
  }
}
