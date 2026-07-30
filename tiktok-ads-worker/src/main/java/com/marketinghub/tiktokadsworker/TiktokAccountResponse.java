package com.marketinghub.tiktokadsworker;

import java.time.Instant;

/** Contrato de saída seguro para exibir uma conta TikTok Ads na tela. */
public record TiktokAccountResponse(
        Long id,
        String name,
        String advertiserId,
        boolean hasAccessToken,
        String maskedAccessToken,
        String appId,
        String clientKey,
        boolean hasAppSecret,
        boolean metricsEnabled,
        boolean publicationEnabled,
        Instant createdAt,
        Instant updatedAt,
        Instant lastDiagnosticAt,
        String lastDiagnosticStatus,
        String lastDiagnosticMessage) {
}
