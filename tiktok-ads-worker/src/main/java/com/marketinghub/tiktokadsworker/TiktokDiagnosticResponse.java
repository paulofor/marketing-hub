package com.marketinghub.tiktokadsworker;

import java.time.Instant;
import java.util.List;

/** Resultado do diagnóstico operacional de uma conta TikTok Ads. */
public record TiktokDiagnosticResponse(
        Long accountId,
        String status,
        String message,
        List<String> checks,
        List<String> blockers,
        Instant checkedAt) {
}
