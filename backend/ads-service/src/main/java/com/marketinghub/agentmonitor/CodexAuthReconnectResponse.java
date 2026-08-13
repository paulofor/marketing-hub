package com.marketinghub.agentmonitor;

import java.time.Instant;

/** Responsabilidade: expor o estado seguro e auditável de uma reconexão Codex. */
public record CodexAuthReconnectResponse(
    Long id,
    Long agentId,
    String agentKey,
    String status,
    String verificationUrl,
    String userCode,
    String requestedBy,
    String detail,
    Instant requestedAt,
    Instant startedAt,
    Instant completedAt) {}
