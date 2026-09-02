package com.marketinghub.agenttask;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Responsabilidade: transportar somente os campos leves de tarefa usados na lista de execuções
 * independentes.
 */
public record AgentTaskIndependentExecutionSummarySnapshot(
    Long id,
    String sourceReference,
    String processActivityId,
    String status,
    String assignedAgentKey,
    String executionError,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    BigDecimal estimatedCostUsd,
    String costEstimationStatus,
    Instant receivedAt,
    Instant updatedAt) {}
