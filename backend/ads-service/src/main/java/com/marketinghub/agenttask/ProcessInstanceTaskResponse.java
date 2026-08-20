package com.marketinghub.agenttask;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: apresentar a situação operacional de uma atividade na instância BPM. */
public record ProcessInstanceTaskResponse(
    Long taskId,
    String activityId,
    String activityName,
    String agentKey,
    String agentNickname,
    String taskStatus,
    String operationalState,
    String stateReason,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    BigDecimal estimatedCostUsd,
    String costEstimationStatus,
    Instant receivedAt,
    Instant deliveredAt) {}
