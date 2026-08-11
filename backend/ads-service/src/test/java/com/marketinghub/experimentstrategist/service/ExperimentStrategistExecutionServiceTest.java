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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/** Responsabilidade: validar fila e congelamento auditavel da pesquisa estrategica. */
class ExperimentStrategistExecutionServiceTest {
  /** Retoma uma lease órfã uma única vez antes de reservar o próximo trabalho. */
  @Test
  void recoversStaleExecutionBeforeClaiming() {
    ExperimentStrategistExecutionRepository repository =
        mock(ExperimentStrategistExecutionRepository.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(7L);
    ExperimentStrategistExecution stale = execution(12L, plan);
    stale.setStartedAt(Instant.now().minusSeconds(600));
    when(repository.findByStatusAndStartedAtBeforeOrderByStartedAtAsc(any(), any()))
        .thenReturn(List.of(stale));
    when(repository.countRecentActiveTelemetry(any(), any())).thenReturn(0L);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(repository.findByStatusOrderByCreatedAtAsc(any(), any(Pageable.class)))
        .thenReturn(List.of(stale));
    ExperimentStrategistExecutionService service =
        new ExperimentStrategistExecutionService(
            repository,
            mock(CommercialPlanService.class),
            mock(ExperimentStrategistContextService.class),
            new ObjectMapper());

    var claimed = service.claim();

    assertThat(claimed.status()).isEqualTo(ExperimentStrategistExecutionStatus.RUNNING);
    assertThat(stale.getErrorMessage()).isNull();
    verify(repository).countRecentActiveTelemetry(any(), any());
  }

  /** Entrega ao MCP as evidencias congeladas da execucao estrategica reservada. */
  @Test
  void exposesFrozenExecutionEvidenceToMcp() {
    ExperimentStrategistExecutionRepository repository =
        mock(ExperimentStrategistExecutionRepository.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(7L);
    ExperimentStrategistExecution execution = new ExperimentStrategistExecution();
    execution.setId(12L);
    execution.setCommercialPlan(plan);
    execution.setStatus(ExperimentStrategistExecutionStatus.RUNNING);
    execution.setResearchQuestion("Qual oferta testar?");
    execution.setEvidenceSnapshot("{\"bottleneck\":\"CHECKOUT\"}");
    when(repository.findById(12L)).thenReturn(Optional.of(execution));
    ExperimentStrategistExecutionService service =
        new ExperimentStrategistExecutionService(
            repository,
            mock(CommercialPlanService.class),
            mock(ExperimentStrategistContextService.class),
            new ObjectMapper());

    var response = service.getExecution(12L);

    assertThat(response.id()).isEqualTo(12L);
    assertThat(response.evidenceSnapshot()).contains("CHECKOUT");
  }

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

  /** Cria uma execução mínima para cenários de lease. */
  private ExperimentStrategistExecution execution(Long id, CommercialPlan plan) {
    ExperimentStrategistExecution value = new ExperimentStrategistExecution();
    value.setId(id);
    value.setCommercialPlan(plan);
    value.setStatus(ExperimentStrategistExecutionStatus.RUNNING);
    value.setAuthorityMode("READ_ONLY_RESEARCH");
    value.setResearchQuestion("Qual oferta testar?");
    value.setEvidenceSnapshot("{}");
    value.setCreatedAt(Instant.now().minusSeconds(700));
    return value;
  }
}
