package com.marketinghub.worker.openai.core.qualityreview;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
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
        @NotBlank String visionModel,
        @NotBlank String imageDetail,
        @NotNull Duration timeout,
        String chromiumExecutablePath,
        @NotNull Duration screenshotTimeout
) {
    /** Normaliza valores opcionais usados pelo worker de revisão visual quando a configuração externa omite o campo. */
    public QualityReviewWorkerProperties {
        if (apiPrefix == null || apiPrefix.isBlank()) {
            apiPrefix = "/api";
        }
        if (visionModel == null || visionModel.isBlank()) {
            visionModel = "gpt-5.5";
        }
        if (imageDetail == null || imageDetail.isBlank()) {
            imageDetail = "original";
        } else {
            imageDetail = imageDetail.trim().toLowerCase();
        }
        if (!List.of("low", "high", "original", "auto").contains(imageDetail)) {
            throw new IllegalArgumentException("qualityreview.worker.image-detail must be one of: low, high, original, auto");
        }
        if (timeout == null) {
            timeout = Duration.ofMinutes(30);
        }
        if (chromiumExecutablePath == null || chromiumExecutablePath.isBlank()) {
            chromiumExecutablePath = "/usr/bin/chromium";
        }
        if (screenshotTimeout == null || screenshotTimeout.isNegative() || screenshotTimeout.isZero()) {
            screenshotTimeout = Duration.ofSeconds(30);
        }
    }
}
