package com.marketinghub.agenttask;

import java.time.Instant;

/** Responsabilidade: entregar ao executor uma atividade BPM elegível e auditável. */
public record AgentTaskPendingResponse(
    Long taskId,
    String agentKey,
    String processCode,
    Integer processVersion,
    String activityId,
    String activityName,
    String title,
    String description,
    String sourceReference,
    Instant receivedAt,
    String processContextJson) {}
