package com.marketinghub.worker.openai.core.imagegeneration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Responsabilidade: concentrar as propriedades operacionais do worker OpenAI da etapa imagegeneration. */
@Validated
@ConfigurationProperties(prefix = "imagegeneration.worker")
public record ImageGenerationWorkerProperties(
        boolean enabled,

        @Min(1)
        int pendingLimit,

        @NotBlank
        String backendBaseUrl,

        String apiPrefix,

        @NotBlank
        String imageModel,

        @NotNull
        Duration timeout,

        @Min(1)
        int uploadAttempts,

        @NotNull
        Duration uploadBackoff,

        String workerId,

        @Min(0)
        int rolloutPercentage,

        double costPerImageUsd
) {
    /** Normaliza valores opcionais usados pelo worker de imagegeneration quando a configuração externa omite o campo. */
    public ImageGenerationWorkerProperties {
        if (apiPrefix == null || apiPrefix.isBlank()) {
            apiPrefix = "/api";
        }
        if (imageModel == null || imageModel.isBlank()) {
            imageModel = "gpt-image-2";
        }
        if (timeout == null) {
            timeout = Duration.ofMinutes(30);
        }
        if (uploadAttempts < 1) {
            uploadAttempts = 3;
        }
        if (uploadBackoff == null || uploadBackoff.isNegative() || uploadBackoff.isZero()) {
            uploadBackoff = Duration.ofMillis(300);
        }
        if (rolloutPercentage > 100) {
            rolloutPercentage = 100;
        }
        if (costPerImageUsd < 0d) {
            costPerImageUsd = 0d;
        }
    }
}
