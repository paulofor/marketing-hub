package com.marketinghub.opsmonitor.service.registerHeartbeat;

import java.time.Instant;

/** Payload enviado pelo worker com o resultado de uma verificação operacional. */
public record RegisterModuleHeartbeatRequest(Instant checkedAt, String status, Integer httpStatus, Long responseTimeMs,
        String errorMessage, String rawPayload) {}
