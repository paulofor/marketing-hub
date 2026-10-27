package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Responsabilidade: impedir retomada de Atena após qualquer indício de execução do modelo. */
class AtenaUnreceivedLeaseTest {
  private static final Instant NOW = Instant.parse("2026-10-27T15:00:00Z");

  /** Recusa auditoria parcial, consumo inclusive zero e entregas que já possuem evidência. */
  @ParameterizedTest
  @MethodSource("protectedStates")
  void protectsAnyRecordedExecution(Consumer<AgentTask> state) {
    AgentTask task = unreceived();
    state.accept(task);
    assertThat(AtenaUnreceivedLease.eligible(task, NOW)).isFalse();
  }

  /** Lista alterações independentes que devem impedir uma inferência duplicada. */
  static Stream<Consumer<AgentTask>> protectedStates() {
    return Stream.of(
        task -> task.setExecutionPrompt("prompt"),
        task -> task.setExecutionAgentPrompt("agente"),
        task -> task.setExecutionActivityPrompt("atividade"),
        task -> task.setExecutionMode("MODEL"),
        task -> task.setExecutionModelCode("modelo"),
        task -> task.setExecutionReasoningEffort("xhigh"),
        task -> task.setResultJson("{}"),
        task -> task.setEvidenceJson("{}"),
        task -> task.setInputTokens(0L),
        task -> task.setCachedInputTokens(0L),
        task -> task.setOutputTokens(0L),
        task -> task.setEstimatedCostUsd(BigDecimal.ZERO),
        task -> task.setModelUsageUpdatedAt(NOW),
        task -> task.setDeliveredAt(NOW),
        task -> task.setStatus("COMPLETED"),
        task -> task.setStatus("BLOCKED"),
        task -> task.setUpdatedAt(NOW.minusSeconds(300)));
  }

  /** Exige idade comprovada e conserva a janela maior que o timeout HTTP do worker. */
  @Test
  void requiresKnownAgeAndProtectsRecentReservations() {
    AgentTask task = unreceived();
    assertThat(AtenaUnreceivedLease.eligible(task, NOW)).isTrue();
    task.setUpdatedAt(null);
    assertThat(AtenaUnreceivedLease.eligible(task, NOW)).isFalse();
    task.setReceivedAt(NOW.minusSeconds(301));
    assertThat(AtenaUnreceivedLease.eligible(task, NOW)).isTrue();
    task.setUpdatedAt(NOW);
    assertThat(AtenaUnreceivedLease.eligible(task, NOW)).isFalse();
  }

  /** Cria uma reserva sem auditoria para simular resposta HTTP perdida. */
  private AgentTask unreceived() {
    AgentTask task = new AgentTask();
    task.setStatus("IN_PROGRESS");
    task.setUpdatedAt(NOW.minusSeconds(600));
    return task;
  }
}
