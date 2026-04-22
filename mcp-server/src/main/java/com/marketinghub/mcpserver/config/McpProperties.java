package com.marketinghub.mcpserver.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mcp")
public record McpProperties(
        @NotBlank String serverName,
        @NotBlank String serverVersion,
        String apiKey,
        @NotNull @Valid Logs logs
) {
    public record Logs(
            @NotBlank String backendPath,
            @NotBlank String aiWorkerPath,
            @NotBlank String leadPortalPath,
            @NotBlank String facebookAdsPath,
            @NotBlank String emailServicePath,
            @NotBlank String leadPortalPaymentPath,
            @Positive int maxLines
    ) {
    }
}
