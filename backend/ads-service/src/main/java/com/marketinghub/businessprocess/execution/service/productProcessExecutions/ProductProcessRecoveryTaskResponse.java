package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionResponse;
import java.time.Instant;

/**
 * Responsabilidade: mostrar a tentativa de correção junto ao comando sem duplicar sua auditoria.
 */
public record ProductProcessRecoveryTaskResponse(
    Long taskId,
    String status,
    String agentName,
    Instant createdAt,
    Instant startedAt,
    Instant finishedAt,
    String executionError,
    String recommendedAction) {

  /** Resume somente informações persistidas da tentativa selecionada pelo backend. */
  public static ProductProcessRecoveryTaskResponse from(
      BusinessProcessActivityExecutionResponse task) {
    return new ProductProcessRecoveryTaskResponse(
        task.taskId(),
        task.status(),
        task.assignedAgentNickname(),
        task.createdAt(),
        task.startedAt(),
        task.finishedAt(),
        task.executionError(),
        task.blockerGuidance() == null ? null : task.blockerGuidance().recommendedAction());
  }
}
