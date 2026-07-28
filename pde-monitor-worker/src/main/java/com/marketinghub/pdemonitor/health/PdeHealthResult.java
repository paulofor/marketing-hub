package com.marketinghub.pdemonitor.health;

import java.time.Instant;

/** Representa o resultado auditável de uma verificação pública de PDE. */
public record PdeHealthResult(
        Instant checkedAt,
        String status,
        Integer httpStatus,
        long responseTimeMs,
        String rawPayload,
        String errorMessage) {

    /** Indica se o resultado permite considerar o PDE saudável para venda 24/7. */
    public boolean online() {
        return "ONLINE".equals(status);
    }
}
