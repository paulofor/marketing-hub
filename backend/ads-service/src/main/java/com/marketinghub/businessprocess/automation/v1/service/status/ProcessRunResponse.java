package com.marketinghub.businessprocess.automation.v1.service.status;

import com.marketinghub.agenttask.AgentTaskResultView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: expor o progresso persistido e os comandos permitidos para o cabeçalho. */
public record ProcessRunResponse(
    Long id,
    Long productId,
    Long processDefinitionId,
    Long chainId,
    Long learningCycleId,
    String sourceReference,
    String status,
    String reason,
    String currentActivityId,
    String currentActivityName,
    String currentOwnerName,
    String currentOwnerPortraitUrl,
    Integer currentSequence,
    int totalActivities,
    int completedActivities,
    int remainingActivities,
    int omittedActivities,
    int completionPercentage,
    BigDecimal knownCostUsd,
    String costCoverage,
    boolean canStart,
    boolean canPause,
    boolean canResume,
    boolean automaticExecution,
    Long childRunId,
    String navigationUrl,
    Instant createdAt,
    Instant updatedAt,
    Instant lastReconciledAt,
    Instant finishedAt,
    long revision,
    List<ProcessRunRelationResponse> parentProcesses,
    List<ProcessRunRelationResponse> subprocesses,
    ProcessRunUserAction userAction,
    ProcessRunQueueBlocker queueBlocker,
    String financialReviewUrl) {
  /**
   * Converte motivos técnicos legados somente na projeção pública, sem alterar o estado persistido.
   */
  public ProcessRunResponse {
    reason = AgentTaskResultView.businessReason(reason);
  }
}
