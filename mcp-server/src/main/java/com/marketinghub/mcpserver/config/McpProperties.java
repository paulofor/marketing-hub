package com.marketinghub.mcpserver.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "mcp")
public record McpProperties(
        @NotBlank String serverName,
        @NotBlank String serverVersion,
        String apiKey,
        @NotNull @Valid Logs logs,
        @NotNull @Valid Meta meta,
        @NotNull @Valid Github github
) {
    public record Logs(
            @NotBlank String backendPath,
            @NotBlank String aiWorkerPath,
            @NotBlank String leadPortalPath,
            @NotBlank String facebookAdsPath,
            @NotBlank String emailServicePath,
            @NotBlank String leadPortalPaymentPath,
            @NotBlank String mdsPath,
            @NotBlank String moisPath,
            @Positive int fetchTimeoutSeconds,
            @Positive int fetchAttempts,
            @Positive int fetchRetryDelayMillis,
            @Positive int maxLines,
            @Positive int httpTailRangeBytes
    ) {
    }

    public record Meta(
            boolean enabled,
            @NotBlank String graphBaseUrl,
            @NotBlank String graphVersion,
            String accessToken,
            String debugAccessToken,
            @NotEmpty List<@NotBlank String> docsAllowedHosts
    ) {
    }

    public record Github(
            boolean enabled,
            @NotBlank String apiBaseUrl,
            @NotBlank String owner,
            @NotBlank String repo,
            String token
    ) {
    }
}

