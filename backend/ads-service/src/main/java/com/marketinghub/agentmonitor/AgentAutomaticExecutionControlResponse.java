package com.marketinghub.agentmonitor;

import java.time.Instant;

/** Responsabilidade: expor a verdade persistida do controle automático de um agente. */
public record AgentAutomaticExecutionControlResponse(
    Long agentId,
    String agentKey,
    boolean automaticExecutionEnabled,
    String automaticExecutionStatus,
    Instant changedAt,
    String changedBy) {}
