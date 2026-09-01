package com.marketinghub.businessprocess.independent.service.executions;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;

/** Contrato auditável de uma tentativa de tarefa pertencente à execução independente. */
public record IndependentBusinessProcessTaskResponse(
    Long taskId,
    Long processDefinitionId,
    Integer processVersionNumber,
    String sourceReference,
    String status,
    String assignedAgentKey,
    String assignedAgentNickname,
    String title,
    JsonNode result,
    JsonNode evidence,
    String executionError,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    BigDecimal estimatedCostUsd,
    String costEstimationStatus,
    String modelCode,
    String executionMode,
    String reasoningEffort,
    String productInternalName,
    String promptSent,
    String agentPromptPart,
    String activityPromptPart,
    Instant createdAt,
    Instant startedAt,
    Instant finishedAt) {}
