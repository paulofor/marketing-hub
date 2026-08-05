package com.marketinghub.agent.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: apresentar maturidade e fechamento de ciclos de um agente. */
public record AgentMaturityDto(
    Long agentId,
    String agentKey,
    String agentName,
    long executions,
    long completedExecutions,
    long failedExecutions,
    long openTasks,
    long resolvedTasks,
    long confirmedResults,
    BigDecimal estimatedCost,
    BigDecimal completionRate,
    BigDecimal resolutionRate,
    Instant lastExecutionAt,
    String maturityLevel,
    String nextMaturityAction) {}
