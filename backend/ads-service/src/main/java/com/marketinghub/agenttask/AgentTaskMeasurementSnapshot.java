package com.marketinghub.agenttask;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Responsabilidade: transportar somente os campos de uma tarefa necessários para medir a cadeia do
 * produto.
 */
public record AgentTaskMeasurementSnapshot(
    Long id,
    Long processDefinitionId,
    String processCode,
    String parentProcessCode,
    String processActivityId,
    String processActivityName,
    String sourceReference,
    String status,
    String activityInstanceStatus,
    Instant createdAt,
    Instant updatedAt,
    Instant deliveredAt,
    String resultJson,
    String evidenceJson,
    BigDecimal estimatedCostUsd) {}
