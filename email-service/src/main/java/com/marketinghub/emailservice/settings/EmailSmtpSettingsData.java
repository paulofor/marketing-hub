package com.marketinghub.emailservice.settings;

public record EmailSmtpSettingsData(
        String providerName,
        String host,
        Integer port,
        Boolean authEnabled,
        String username,
        String password,
        String fromName,
        String fromEmail,
        Boolean useStartTls,
        Boolean useSsl,
        Integer connectionTimeoutMs,
        Integer readTimeoutMs,
        Integer writeTimeoutMs,
        Boolean dryRun
) {
}
