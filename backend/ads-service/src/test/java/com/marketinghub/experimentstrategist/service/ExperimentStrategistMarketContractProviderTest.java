package com.marketinghub.experimentstrategist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar autoria, integridade e segregação do contrato estratégico de Atena. */
class ExperimentStrategistMarketContractProviderTest {
  /** Publica a versão, execução e hash do último contrato concluído do plano. */
  @Test
  void exposesLatestCompletedMarketContractWithIntegrityHash() {
    ExperimentStrategistExecutionRepository executions =
        mock(ExperimentStrategistExecutionRepository.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(4L);
    ExperimentStrategistExecution execution = execution(plan);
    when(executions.findFirstByCommercialPlanIdAndStatusAndAuthorityModeOrderByFinishedAtDescIdDesc(
            4L, ExperimentStrategistExecutionStatus.COMPLETED, "READ_ONLY_RESEARCH"))
        .thenReturn(Optional.of(execution));
    ExperimentStrategistMarketContractProvider provider =
        new ExperimentStrategistMarketContractProvider(
            executions, mock(CommercialPlanRepository.class), new ObjectMapper());

    var result = provider.resolve("commercial-plan:4@v2").orElseThrow();

    assertThat(result).containsEntry("availability", "AVAILABLE");
    assertThat(result).containsEntry("sourceAgent", "ATENA");
    assertThat(result).containsEntry("strategistExecutionId", 41L);
    assertThat(result.get("contentHash").toString()).hasSize(64);
    assertThat(result.get("contract").toString())
        .contains("READY_FOR_OPERATION", "Profissionais locais");
  }

  /** Resolve o mesmo plano por experimento sem herdar estratégia de outro portfólio. */
  @Test
  void resolvesExperimentReferenceThroughItsCommercialPlan() {
    ExperimentStrategistExecutionRepository executions =
        mock(ExperimentStrategistExecutionRepository.class);
    CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(4L);
    when(plans.findByExperimentReference(89L)).thenReturn(List.of(plan));
    when(executions.findFirstByCommercialPlanIdAndStatusAndAuthorityModeOrderByFinishedAtDescIdDesc(
            4L, ExperimentStrategistExecutionStatus.COMPLETED, "READ_ONLY_RESEARCH"))
        .thenReturn(Optional.of(execution(plan)));
    ExperimentStrategistMarketContractProvider provider =
        new ExperimentStrategistMarketContractProvider(executions, plans, new ObjectMapper());

    assertThat(provider.resolve("experiment:89").orElseThrow())
        .containsEntry("strategistExecutionId", 41L);
    assertThat(provider.resolve("experiment:90")).isEmpty();
  }

  /** Declara lacuna quando a última pesquisa histórica não contém o artefato v2. */
  @Test
  void marksHistoricalResearchWithoutFormalContractAsMissing() {
    ExperimentStrategistExecutionRepository executions =
        mock(ExperimentStrategistExecutionRepository.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(4L);
    ExperimentStrategistExecution execution = execution(plan);
    execution.setRecommendationJson("{\"recommendation\":{\"selectedAlternative\":\"A\"}}");
    when(executions.findFirstByCommercialPlanIdAndStatusAndAuthorityModeOrderByFinishedAtDescIdDesc(
            4L, ExperimentStrategistExecutionStatus.COMPLETED, "READ_ONLY_RESEARCH"))
        .thenReturn(Optional.of(execution));
    ExperimentStrategistMarketContractProvider provider =
        new ExperimentStrategistMarketContractProvider(
            executions, mock(CommercialPlanRepository.class), new ObjectMapper());

    var result = provider.resolveForPlan(4L);

    assertThat(result).containsEntry("availability", "MISSING");
    assertThat(result.get("reason").toString()).contains("histórica", "v2");
  }

  /** Monta uma execução estratégica válida e concluída. */
  private ExperimentStrategistExecution execution(CommercialPlan plan) {
    ExperimentStrategistExecution execution = new ExperimentStrategistExecution();
    execution.setId(41L);
    execution.setCommercialPlan(plan);
    execution.setStatus(ExperimentStrategistExecutionStatus.COMPLETED);
    execution.setAuthorityMode("READ_ONLY_RESEARCH");
    execution.setFinishedAt(Instant.parse("2026-08-28T10:00:00Z"));
    execution.setRecommendationJson(
        "{\"marketStrategicContract\":{\"contractVersion\":\"MARKET_STRATEGY_V2\","
            + "\"status\":\"READY_FOR_OPERATION\",\"buyer\":\"Profissionais locais\"}}");
    return execution;
  }
}
