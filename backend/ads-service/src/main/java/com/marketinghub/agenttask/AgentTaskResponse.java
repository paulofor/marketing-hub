package com.marketinghub.agenttask;

import java.time.Instant;

/** Responsabilidade: expor uma tarefa com identidades legíveis e técnicas dos agentes. */
public record AgentTaskResponse(
    Long id,
    Long assignedAgentId,
    String assignedAgentKey,
    String assignedAgentNickname,
    String requestedByType,
    Long requestedByAgentId,
    String requestedByAgentKey,
    String requestedByName,
    String title,
    String description,
    String priority,
    String status,
    String sourceReference,
    Instant createdAt,
    Instant updatedAt) {}
