package com.marketinghub.settings.email;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(toBuilder = true)
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
