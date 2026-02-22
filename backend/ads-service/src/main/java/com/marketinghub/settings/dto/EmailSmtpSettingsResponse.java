package com.marketinghub.settings.dto;

import java.time.Instant;

public record EmailSmtpSettingsResponse(
        String providerName,
        String host,
        Integer port,
        boolean authEnabled,
        String username,
        String fromName,
        String fromEmail,
        boolean useStartTls,
        boolean useSsl,
        Integer connectionTimeoutMs,
        Integer readTimeoutMs,
        Integer writeTimeoutMs,
        boolean dryRun,
        boolean hasPassword,
        Instant updatedAt
) {
}
