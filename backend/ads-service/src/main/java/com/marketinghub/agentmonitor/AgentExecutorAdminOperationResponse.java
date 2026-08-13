package com.marketinghub.agentmonitor;

import java.time.Instant;

/** Responsabilidade: expor o andamento seguro de uma atualização ou reinício de executor. */
public record AgentExecutorAdminOperationResponse(
    Long id,
    Long agentId,
    String agentKey,
    String operationType,
    String status,
    String requestedBy,
    Instant requestedAt,
    Instant startedAt,
    Instant completedAt,
    String detail) {}
