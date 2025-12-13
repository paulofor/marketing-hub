package com.marketinghub.emailservice.config;

import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "lead-portal.dispatch")
public record LeadPortalDispatchProperties(
        boolean enabled,
        @Positive int batchSize,
        @Positive long initialDelay,
        @Positive long pollInterval,
        /**
         * Exportações podem levar tempo para ler ZIPs grandes do storage.
         */
        @Positive long readTimeout
) {

    public Duration readTimeoutDuration() {
        return Duration.ofMillis(readTimeout);
    }
}
