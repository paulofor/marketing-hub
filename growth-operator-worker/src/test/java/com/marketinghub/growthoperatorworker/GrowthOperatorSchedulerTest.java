package com.marketinghub.growthoperatorworker;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: proteger o consumo independente da fila principal do Operador de Crescimento.
 */
class GrowthOperatorSchedulerTest {
  /** Confirma que falha no ciclo automatico nao impede a reserva de uma pendencia existente. */
  @Test
  void shouldClaimPendingWhenAutomaticCycleIsUnavailable() {
    GrowthOperatorBackendClient backendClient = mock(GrowthOperatorBackendClient.class);
    CodexReadOnlyRunner runner = mock(CodexReadOnlyRunner.class);
    WorkerProperties properties = new WorkerProperties();
    properties.setCommercialPlanId(2L);
    doThrow(new IllegalStateException("Nenhum experimento RUNNING"))
        .when(backendClient)
        .ensureAutomaticCycle(2L);
    when(backendClient.claimPending()).thenReturn(null);

    new GrowthOperatorScheduler(backendClient, runner, properties).processOne();

    verify(backendClient).claimPending();
  }
}
