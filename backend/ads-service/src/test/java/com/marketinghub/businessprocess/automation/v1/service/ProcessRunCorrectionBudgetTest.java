package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCyclePreparationBudget;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.CycleFinancialReview;
import com.marketinghub.repository.jdbc.learningcycle.CyclePreparationCostRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: impedir retrabalho pago sem recibo válido e cobertura financeira do ciclo. */
class ProcessRunCorrectionBudgetTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningCyclePreparationBudget budgets = mock(LearningCyclePreparationBudget.class);
  private final CyclePreparationCostRepository costs = mock(CyclePreparationCostRepository.class);
  private final ProcessRunCorrectionBudget guard =
      new ProcessRunCorrectionBudget(cycles, budgets, costs);
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final ProcessRun run = new ProcessRun();

  /** Replica a ausência de recibo em Capella e em outra identidade, sem autorizar por texto. */
  @ParameterizedTest
  @ValueSource(longs = {7, 99107})
  void requiresOwnReceiptBeforeAnyNewCorrection(long productId) {
    prepare(productId, "64.289364");
    assertThat(guard.blockReason(run, activity()))
        .contains("falta o teto", "demonstração financeira");
    verifyNoInteractions(costs);
  }

  /** Custo já consumido e reserva impedem repetir a tarefa mesmo com recibo próprio. */
  @ParameterizedTest
  @ValueSource(strings = {"50.00", "64.29", "70.00"})
  void refusesExhaustedOrInsufficientBalance(String limit) {
    prepare(7L, "64.289364");
    authorize(limit, "5.00");
    assertThat(guard.blockReason(run, activity())).contains("saldo estimado", "não cobre");
  }

  /** Um limite suficiente permite a correção sem aumentar mídia ou transferir outros recibos. */
  @Test
  void acceptsAvailableBalanceAfterCostsAndReserve() {
    prepare(99107L, "64.289364");
    authorize("75.00", "5.00");
    assertThat(guard.blockReason(run, activity())).isNull();
    verify(costs).summarize(99107L);
  }

  /** A ausência total de estimativas não é tratada como consumo zero. */
  @Test
  void keepsUnknownCostsVisible() {
    prepare(7L, null);
    authorize("75.00", "5.00");
    assertThat(guard.blockReason(run, activity()))
        .contains("custo acumulado ainda não foi informado");
  }

  /** Recusa produto, cadeia ou referência divergentes antes de consultar o financeiro alheio. */
  @Test
  void isolatesProductCycleAndExperiment() {
    prepare(7L, "64.00");
    cycle.setProductId(8L);
    assertThat(guard.blockReason(run, activity())).contains("mesmo contexto");
    verifyNoInteractions(budgets, costs);
  }

  /** Omitir o ciclo não contorna a autorização quando a referência identifica o experimento. */
  @Test
  void resolvesCycleFromExperimentWithoutInheritingAuthorization() {
    prepare(7L, "64.00");
    run.setLearningCycleId(null);
    when(cycles.findByExperimentId(112L)).thenReturn(Optional.of(cycle));
    assertThat(guard.blockReason(run, activity())).contains("falta o teto");
  }

  /** Fluxos independentes sem experimento continuam regidos por seu contrato próprio. */
  @Test
  void preservesIndependentFlowWithoutLearningCycle() {
    run.setSourceReference("product:99001@agent-validation-v1");
    assertThat(guard.blockReason(run, activity())).isNull();
    verifyNoInteractions(cycles, budgets, costs);
  }

  /** Preserva experimentos antigos que não pertencem ao fluxo de ciclos. */
  @Test
  void preservesLegacyExperimentOutsideLearningCycles() {
    run.setSourceReference("experiment:99001");
    when(cycles.findByExperimentId(99001L)).thenReturn(Optional.empty());
    assertThat(guard.blockReason(run, activity())).isNull();
    verifyNoInteractions(budgets, costs);
  }

  /** Confirma que o contexto de produção consulta a mesma proteção financeira testada. */
  @Test
  void productionContextUsesTheSameFinancialGuard() {
    prepare(7L, "64.289364");
    var context =
        new ProcessRunContext(null, null, null, null, null, new ObjectMapper(), null, null);
    org.springframework.test.util.ReflectionTestUtils.setField(context, "correctionBudget", guard);
    assertThat(context.correctionBudgetBlockReason(run, activity())).contains("falta o teto");
  }

  /** Monta identidades isoladas e custos agregados, sem auditorias, modelos ou banco externo. */
  private void prepare(long productId, String amount) {
    cycle.setId(19L);
    cycle.setProductId(productId);
    cycle.setChainDefinitionId(27L);
    cycle.setExperimentId(112L);
    cycle.setStatus("OPEN");
    cycle.setProductVersion("candidata-v3");
    run.setProductId(productId);
    run.setLearningCycleId(19L);
    run.setChainDefinitionId(27L);
    run.setProcessDefinitionId(126L);
    run.setSourceReference("experiment:112");
    when(cycles.findById(19L)).thenReturn(Optional.of(cycle));
    when(costs.summarize(productId))
        .thenReturn(
            List.of(
                new CycleFinancialReview.CostSlice(
                    112L, amount == null ? null : new BigDecimal(amount), 3)));
  }

  /**
   * Simula apenas recibo explícito elegível; a validação de versão pertence ao ledger existente.
   */
  private void authorize(String limit, String reserve) {
    when(budgets.current(cycle))
        .thenReturn(
            new CycleFinancialReview.Authorization(
                1L,
                "internal://qa/budget",
                new BigDecimal(limit),
                new BigDecimal(reserve),
                "Operador QA",
                "Reserva explícita de QA",
                Instant.now(),
                "candidata-v3",
                true));
  }

  /** Oferece o custo conhecido de uma tentativa da mesma atividade e referência. */
  private ProductProcessActivityExecutionGroupResponse activity() {
    return new ObjectMapper()
        .convertValue(
            Map.of(
                "tasks",
                List.of(
                    Map.of(
                        "taskId",
                        882,
                        "processDefinitionId",
                        126,
                        "sourceReference",
                        "experiment:112",
                        "estimatedCostUsd",
                        new BigDecimal("2.605238")))),
            ProductProcessActivityExecutionGroupResponse.class);
  }
}
