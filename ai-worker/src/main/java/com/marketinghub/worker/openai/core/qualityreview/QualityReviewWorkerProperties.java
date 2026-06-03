package com.marketinghub.worker.openai.core.qualityreview;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Responsabilidade: concentrar as propriedades operacionais do worker OpenAI da revisão visual. */
@Validated
@ConfigurationProperties(prefix = "qualityreview.worker")
public record QualityReviewWorkerProperties(
        boolean enabled,
        @Min(1) int pendingLimit,
        @NotBlank String backendBaseUrl,
        String apiPrefix,
        @NotBlank String promptResource,
        @NotBlank String schemaResource,
        @NotBlank String schemaName,
        @NotNull Duration timeout
) {
    /** Normaliza valores opcionais usados pelo worker de revisão visual quando a configuração externa omite o campo. */
    public QualityReviewWorkerProperties {
        if (apiPrefix == null || apiPrefix.isBlank()) {
            apiPrefix = "/api";
        }
        if (timeout == null) {
            timeout = Duration.ofMinutes(30);
        }
    }
}
