package com.marketinghub.financialagentworker;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Responsabilidade: proteger a independência entre as filas operacionais de Plutus. */
class FinancialAgentSchedulerTest {
  /** Continua a conciliação quando a fila auxiliar de vídeo estiver indisponível. */
  @Test
  void continuesFinancialQueueWhenVideoQueueFails() {
    FinancialAgentBackendClient backend = mock(FinancialAgentBackendClient.class);
    FinancialCodexRunner runner = mock(FinancialCodexRunner.class);
    FinancialAgentProperties properties = new FinancialAgentProperties();
    when(backend.pendingVideoCycle()).thenThrow(new IllegalStateException("endpoint indisponível"));
    when(backend.claimPending()).thenReturn(null);
    FinancialAgentScheduler scheduler = new FinancialAgentScheduler(backend, runner, properties);

    scheduler.processOne();

    verify(backend).reconcileVideoFinancialReviews();
    verify(backend).claimPending();
  }

  /** Continua as demais filas quando a reconciliação de reservas de vídeo estiver indisponível. */
  @Test
  void continuesFinancialQueueWhenVideoReconciliationFails() {
    FinancialAgentBackendClient backend = mock(FinancialAgentBackendClient.class);
    FinancialCodexRunner runner = mock(FinancialCodexRunner.class);
    FinancialAgentProperties properties = new FinancialAgentProperties();
    org.mockito.Mockito.doThrow(new IllegalStateException("endpoint indisponível"))
        .when(backend)
        .reconcileVideoFinancialReviews();
    when(backend.claimPending()).thenReturn(null);
    FinancialAgentScheduler scheduler = new FinancialAgentScheduler(backend, runner, properties);

    scheduler.processOne();

    verify(backend).pendingVideoCycle();
    verify(backend).claimPending();
  }

  /** Reutiliza a resposta persistida quando o callback de decisão anterior foi interrompido. */
  @Test
  void reusesAuditedVideoReviewWithoutCallingModelAgain() throws Exception {
    FinancialAgentBackendClient backend = mock(FinancialAgentBackendClient.class);
    FinancialCodexRunner runner = mock(FinancialCodexRunner.class);
    FinancialAgentProperties properties = new FinancialAgentProperties();
    String raw = "{\"decision\":\"APPROVED\"}";
    VideoProductionCycleReview cycle =
        new VideoProductionCycleReview(
            91L,
            37L,
            4L,
            3L,
            91L,
            "PENDING_FINANCIAL_REVIEW",
            BigDecimal.TEN,
            BigDecimal.ZERO,
            "{}",
            322L,
            raw);
    when(backend.pendingVideoCycle()).thenReturn(cycle);
    when(runner.videoDecision(raw, cycle)).thenReturn(Map.of("decision", "APPROVED"));
    FinancialAgentScheduler scheduler = new FinancialAgentScheduler(backend, runner, properties);

    scheduler.processOne();

    InOrder order = inOrder(backend);
    order.verify(backend).reconcileVideoFinancialReviews();
    order.verify(backend).pendingVideoCycle();
    verify(runner).videoDecision(raw, cycle);
    verify(backend).decideVideoCycle(91L, Map.of("decision", "APPROVED"));
    verify(runner, org.mockito.Mockito.never()).reviewVideoCycle(cycle);
  }
}
