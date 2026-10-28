package com.marketinghub.agentmonitor;

import java.time.Instant;

/** Resume o trabalho atual sem carregar prompts, respostas ou o histórico do agente. */
public record AgentWorkTaskSnapshot(
    Long id,
    String status,
    String title,
    String description,
    String taskKind,
    String gateStatus,
    String gateCode,
    String sourceReference,
    Instant updatedAt) {}
