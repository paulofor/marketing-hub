package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.ProcessRunEvent;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview.CycleFinancialReview;
import com.marketinghub.repository.jdbc.learningcycle.CyclePreparationCostRepository;
import com.marketinghub.repository.jpa.processautomation.ProcessRunEventRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: explicar a pausa explícita e impedir decisões financeiras inferidas pelo
 * histórico.
 */
class ProcessRunPauseGuidanceTest {
  private final ProcessRunEventRepository events = mock(ProcessRunEventRepository.class);
  private final CyclePreparationCostRepository costs = mock(CyclePreparationCostRepository.class);
  private final ProcessRunPauseGuidance guidance =
      new ProcessRunPauseGuidance(events, costs, new ObjectMapper());

  /** Reproduz a lacuna da Vega e outra identidade sem misturar custos nem referência financeira. */
  @ParameterizedTest
  @ValueSource(longs = {4, 99004})
  void explainsOnlyExplicitFinancialPause(long productId) {
    var run = run(productId);
    var event = new ProcessRunEvent();
    event.setId(100L);
    event.setDetailsJson(
        "{\"reasonCode\":\"COST_RECONCILIATION\",\"reason\":\"Conferir antes de gastar.\"}");
    when(events.findFirstByRunIdAndEventTypeOrderByIdDesc(run.getId(), "PAUSE_CONTEXT"))
        .thenReturn(Optional.of(event));
    when(costs.summarize(productId))
        .thenReturn(
            List.of(new CycleFinancialReview.CostSlice(115L, new BigDecimal("113.33"), 10)));
    var action = guidance.resolve(run);
    assertThat(action.reason())
        .contains("10 tarefas antigas", "saldo disponível ainda não é conhecido");
    assertThat(action.actionUrl()).contains("productId=" + productId, "chainId=27", "cycleId=22");
    verify(costs).summarize(productId);
    when(costs.waivedUnknownTasks(productId)).thenReturn(9L);
    assertThat(guidance.resolve(run).reason()).contains("1 tarefas antigas");
    when(costs.waivedUnknownTasks(productId)).thenReturn(10L);
    assertThat(guidance.resolve(run)).isNull();
    when(costs.waivedUnknownTasks(productId)).thenReturn(0L);
    when(costs.summarize(productId))
        .thenReturn(List.of(new CycleFinancialReview.CostSlice(115L, new BigDecimal("114.33"), 0)));
    assertThat(guidance.resolve(run)).isNull();
    run.setStatus("WAITING_ACTIVITY");
    assertThat(guidance.resolve(run)).isNull();
  }

  /** Pausa comum e consulta sem motivo não fabricam decisão humana a partir de custos parciais. */
  @Test
  void preservesOrdinaryPauseAndMissingHistoricalReason() {
    var run = run(4);
    assertThat(guidance.resolve(run)).isNull();
    var event = new ProcessRunEvent();
    event.setDetailsJson("{\"reasonCode\":\"OPERATOR\"}");
    when(events.findFirstByRunIdAndEventTypeOrderByIdDesc(run.getId(), "PAUSE_CONTEXT"))
        .thenReturn(Optional.of(event));
    assertThat(guidance.resolve(run)).isNull();
    verifyNoInteractions(costs);
  }

  /** Falha real de leitura impede declarar que a pausa foi liberada. */
  @Test
  void invalidAuditFailsVisibly() {
    var event = new ProcessRunEvent();
    event.setDetailsJson("invalid");
    when(events.findFirstByRunIdAndEventTypeOrderByIdDesc(102L, "PAUSE_CONTEXT"))
        .thenReturn(Optional.of(event));
    assertThatThrownBy(() -> guidance.resolve(run(4))).hasMessageContaining("motivo da pausa");
  }

  /** Monta o contexto local exato da execução sem tocar na operação publicada. */
  private ProcessRun run(long productId) {
    var run = new ProcessRun();
    run.setId(102L);
    run.setProductId(productId);
    run.setChainDefinitionId(27L);
    run.setLearningCycleId(22L);
    run.setStatus("PAUSED");
    return run;
  }
}
