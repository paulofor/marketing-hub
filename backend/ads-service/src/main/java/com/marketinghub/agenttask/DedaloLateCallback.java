package com.marketinghub.agenttask;

import java.util.Objects;

/**
 * Responsabilidade: reconhecer uma entrega original de Dédalo bloqueada somente por lease perdida.
 */
final class DedaloLateCallback {
  /** Impede instanciação da política sem estado. */
  private DedaloLateCallback() {}

  /** Exige identidade e auditoria imutáveis e ausência de resultado ou custo já contabilizado. */
  static boolean recoverable(AgentTask task, AgentTaskExecutionAuditRequest audit) {
    return task.getAssignedAgent() != null
        && "landing-generator".equals(task.getAssignedAgent().getAgentKey())
        && "BLOCKED".equals(task.getStatus())
        && task.getExecutionError() != null
        && task.getExecutionError().startsWith("DEDALO_LEASE_RECOVERY_EXHAUSTED|")
        && task.getProcessDefinition() != null
        && present(task.getProcessActivityId())
        && present(task.getSourceReference())
        && !present(task.getResultJson())
        && !present(task.getEvidenceJson())
        && task.getDeliveredAt() == null
        && task.getModelUsageUpdatedAt() == null
        && task.getInputTokens() == null
        && task.getCachedInputTokens() == null
        && task.getOutputTokens() == null
        && task.getEstimatedCostUsd() == null
        && audit != null
        && "MODEL".equals(audit.executionMode())
        && "MODEL".equals(task.getExecutionMode())
        && present(task.getExecutionPrompt())
        && Objects.equals(task.getExecutionModelCode(), audit.modelCode())
        && Objects.equals(task.getExecutionReasoningEffort(), audit.reasoningEffort())
        && Objects.equals(task.getExecutionPrompt(), audit.promptSent())
        && Objects.equals(task.getExecutionAgentPrompt(), audit.agentPromptPart())
        && Objects.equals(task.getExecutionActivityPrompt(), audit.activityPromptPart());
  }

  /** Distingue texto auditável de ausência legada sem normalizar ou reescrever a prova. */
  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }
}
