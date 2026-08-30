package com.marketinghub.businessprocess.independent.service.executions;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Contrato consolidado para acompanhar uma execução independente sem recomputação no frontend. */
public record IndependentBusinessProcessExecutionSummaryResponse(
    Long id,
    UUID requestKey,
    Long processDefinitionId,
    String processCode,
    String processName,
    Integer processVersionNumber,
    String sourceReference,
    String displayName,
    String requestedByName,
    JsonNode input,
    String status,
    int activityCount,
    int completedActivityCount,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    BigDecimal estimatedCostUsd,
    String costCoverage,
    String latestError,
    Instant createdAt,
    Instant startedAt,
    Instant finishedAt) {}
