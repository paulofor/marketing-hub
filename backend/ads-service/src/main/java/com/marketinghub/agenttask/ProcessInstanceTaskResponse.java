package com.marketinghub.agenttask;

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
    Instant receivedAt,
    Instant deliveredAt) {}
