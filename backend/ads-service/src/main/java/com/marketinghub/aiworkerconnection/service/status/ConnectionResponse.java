package com.marketinghub.aiworkerconnection.service.status;

import java.time.Instant;

/** Expõe somente o estado administrativo e os dados temporários necessários ao operador. */
public record ConnectionResponse(
    Long id,
    String status,
    boolean connected,
    boolean workerAvailable,
    Instant checkedAt,
    String verificationUrl,
    String userCode,
    Instant requestedAt,
    Instant expiresAt) {}
