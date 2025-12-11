package com.marketinghub.emailservice.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "lead-portal.dispatch")
public record LeadPortalDispatchProperties(
        boolean enabled,
        @Positive int batchSize,
        @Positive long initialDelay,
        @Positive long pollInterval
) {
}
