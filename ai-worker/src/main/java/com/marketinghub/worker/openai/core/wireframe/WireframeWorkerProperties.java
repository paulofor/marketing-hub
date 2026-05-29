package com.marketinghub.worker.openai.core.wireframe;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "wireframe.worker")
public record WireframeWorkerProperties(
        boolean enabled,

        @Min(1)
        int pendingLimit,

        @NotBlank
        String backendBaseUrl,

        String apiPrefix,

        @NotBlank
        String promptResource,

        @NotBlank
        String schemaResource,

        @NotBlank
        String schemaName,

        @NotNull
        Duration timeout,

        @NotBlank
        String cron
) {
    public WireframeWorkerProperties {
        if (apiPrefix == null || apiPrefix.isBlank()) {
            apiPrefix = "/api";
        }
        if (timeout == null) {
            timeout = Duration.ofMinutes(30);
        }
        if (cron == null || cron.isBlank()) {
            cron = "0 */30 * * * *";
        }
    }
}
