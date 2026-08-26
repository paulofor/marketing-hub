package com.marketinghub.businessprocess.execution.service.recentExecutions;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: apresentar uma tarefa BPM com sua auditoria operacional e de modelo. */
public record BusinessProcessActivityExecutionResponse(
    Long taskId,
    Long processDefinitionId,
    Integer processVersionNumber,
    String title,
    String status,
    String sourceReference,
    String assignedAgentKey,
    String assignedAgentNickname,
    String comments,
    String evidenceJson,
    String executionError,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    BigDecimal estimatedCostUsd,
    String costEstimationStatus,
    Instant createdAt,
    Instant startedAt,
    Instant finishedAt,
    String modelCode,
    String reasoningEffort,
    String productInternalName,
    String promptSent) {}
