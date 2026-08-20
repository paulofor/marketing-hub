package com.marketinghub.landinggeneratoragent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: proteger a retomada operacional das execuções de Dédalo. */
class LandingGeneratorAgentSchedulerTest {
  /** Reserva, executa e reporta uma landing concluída ao backend canônico. */
  @Test
  void reportsSuccessfulResultToBackend() throws Exception {
    LandingGeneratorBackendClient backend = mock(LandingGeneratorBackendClient.class);
    LandingGeneratorCodexRunner runner = mock(LandingGeneratorCodexRunner.class);
    LandingAgentJob job = new LandingAgentJob("job-88", 88L, Map.of());
    Map<String, Object> result = Map.of("decisionJson", "{\"decision\":\"generated\"}");
    when(backend.claimPending()).thenReturn(java.util.List.of(job));
    when(runner.run(job)).thenReturn(result);
    LandingGeneratorAgentScheduler scheduler = new LandingGeneratorAgentScheduler(backend, runner);

    scheduler.processPending();

    verify(backend).report(job, result);
    verify(backend, never()).fail(any(), any());
  }

  /** Preserva a lease quando o timeout for recuperável em vez de registrar falha definitiva. */
  @Test
  void preservesLeaseAfterRecoverableTimeout() throws Exception {
    LandingGeneratorBackendClient backend = mock(LandingGeneratorBackendClient.class);
    LandingGeneratorCodexRunner runner = mock(LandingGeneratorCodexRunner.class);
    LandingAgentJob job = new LandingAgentJob("job-88", 88L, Map.of());
    when(backend.claimPending()).thenReturn(java.util.List.of(job));
    when(runner.run(job)).thenThrow(new CodexActivityTimeoutException("sem atividade"));
    LandingGeneratorAgentScheduler scheduler = new LandingGeneratorAgentScheduler(backend, runner);

    scheduler.processPending();

    verify(backend, never()).fail(any(), any());
  }

  /** Impede até a consulta da fila funcional quando Dédalo está em STOP. */
  @Test
  void doesNotClaimWorkWhenAutomaticExecutionIsStopped() {
    LandingGeneratorBackendClient backend = mock(LandingGeneratorBackendClient.class);
    LandingGeneratorCodexRunner runner = mock(LandingGeneratorCodexRunner.class);
    AutomaticExecutionControl control = mock(AutomaticExecutionControl.class);
    when(control.allowsAutomaticExecution()).thenReturn(false);
    LandingGeneratorAgentScheduler scheduler = new LandingGeneratorAgentScheduler(backend, runner);
    ReflectionTestUtils.setField(scheduler, "automaticExecution", control);

    scheduler.processPending();

    verify(backend, never()).claimPending();
  }
}
