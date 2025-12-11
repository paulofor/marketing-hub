package com.marketinghub.emailservice.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "clients.cloudflare")
public record CloudflareClientProperties(
        @NotBlank String baseUrl,
        String deliveryBaseUrl,
        String deliveryHash,
        String defaultVariant,
        String authToken,
        @Positive long connectTimeout,
        @Positive long readTimeout
) {
    public Duration connectTimeoutDuration() {
        return Duration.ofMillis(connectTimeout);
    }

    public Duration readTimeoutDuration() {
        return Duration.ofMillis(readTimeout);
    }
}
