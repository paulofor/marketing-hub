package com.marketinghub.agenttask;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Responsabilidade: transportar o resumo leve de uma tarefa para listar execuções sem carregar
 * provas extensas.
 */
public record AgentTaskProcessExecutionListSnapshot(
    Long taskId,
    Long processDefinitionId,
    String processCode,
    Integer processVersionNumber,
    String title,
    String status,
    String sourceReference,
    String assignedAgentKey,
    String assignedAgentNickname,
    String processActivityId,
    String processActivityName,
    String executionError,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    BigDecimal estimatedCostUsd,
    String costEstimationStatus,
    Instant createdAt,
    Instant receivedAt,
    Instant deliveredAt,
    Instant updatedAt,
    String executionModelCode,
    String executionMode,
    String executionReasoningEffort,
    String blockerCategory,
    String blockerAction,
    Long activityInstanceProcessDefinitionId) {}
