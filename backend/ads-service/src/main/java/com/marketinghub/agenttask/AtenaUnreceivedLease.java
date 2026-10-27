package com.marketinghub.agenttask;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * Responsabilidade: reconhecer reserva de Atena perdida antes da auditoria obrigatória do modelo.
 */
final class AtenaUnreceivedLease {
  static final String RECOVERED = "ATENA_UNRECEIVED_LEASE_RECOVERY_ONCE|";
  private static final Duration GRACE = Duration.ofMinutes(5);

  /** Impede instanciação da política sem estado. */
  private AtenaUnreceivedLease() {}

  /**
   * Só admite reentrega anterior ao modelo; qualquer evidência ou consumo impede inferência nova.
   */
  static boolean eligible(AgentTask task, Instant now) {
    if (!"IN_PROGRESS".equals(task.getStatus())
        || task.getDeliveredAt() != null
        || task.getInputTokens() != null
        || task.getCachedInputTokens() != null
        || task.getOutputTokens() != null
        || task.getEstimatedCostUsd() != null
        || task.getModelUsageUpdatedAt() != null
        || Stream.of(
                task.getExecutionPrompt(),
                task.getExecutionAgentPrompt(),
                task.getExecutionActivityPrompt(),
                task.getExecutionMode(),
                task.getExecutionModelCode(),
                task.getExecutionReasoningEffort(),
                task.getResultJson(),
                task.getEvidenceJson())
            .anyMatch(value -> value != null && !value.isBlank())) return false;
    Instant lastProgress = task.getUpdatedAt() == null ? task.getReceivedAt() : task.getUpdatedAt();
    return lastProgress != null && lastProgress.plus(GRACE).isBefore(now);
  }
}
