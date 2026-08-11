package com.marketinghub.financialagentworker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

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

    verify(backend).claimPending();
  }
}
