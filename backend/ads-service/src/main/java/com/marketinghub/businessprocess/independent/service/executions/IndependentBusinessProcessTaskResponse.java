package com.marketinghub.businessprocess.independent.service.executions;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;

/** Contrato auditável de uma tentativa de tarefa pertencente à execução independente. */
public record IndependentBusinessProcessTaskResponse(
    Long taskId,
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
    Instant createdAt,
    Instant startedAt,
    Instant finishedAt) {}
