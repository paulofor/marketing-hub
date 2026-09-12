package com.marketinghub.agenttask;

import java.time.Instant;

/** Responsabilidade: transportar provas funcionais sem hidratar prompts ou auditoria técnica. */
public record AgentTaskFunctionalSnapshot(
    Long id,
    Long processDefinitionId,
    String processCode,
    String processActivityId,
    String agentKey,
    String status,
    Instant createdAt,
    Instant deliveredAt,
    String resultJson) {}
