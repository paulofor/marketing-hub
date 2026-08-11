package com.marketinghub.systemimprovement;

import java.time.Instant;

/** Resposta auditável do cadastro de melhorias sugeridas pelos agentes. */
public record SystemImprovementResponse(
    Long id,
    Long requestedByAgentId,
    String agentKey,
    String agentNickname,
    String title,
    String description,
    String taskReference,
    String status,
    Instant requestedAt) {}
