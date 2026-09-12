package com.marketinghub.businessprocess.automation.v1.service.status;

import java.math.BigDecimal;
import java.time.Instant;

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
    long revision) {}
