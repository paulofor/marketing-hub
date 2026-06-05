package com.marketinghub.worker.openai.core.imageplanning;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Responsabilidade: concentrar as propriedades operacionais do worker OpenAI da etapa imageplanning. */
@Validated
@ConfigurationProperties(prefix = "imageplanning.worker")
public record ImagePlanningWorkerProperties(
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

        @NotBlank
        String model,

        @NotNull
        Duration timeout
) {
    /** Normaliza valores opcionais usados pelo worker de imageplanning quando a configuração externa omite o campo. */
    public ImagePlanningWorkerProperties {
        if (apiPrefix == null || apiPrefix.isBlank()) {
            apiPrefix = "/api";
        }
        if (model == null || model.isBlank()) {
            model = "gpt-5.4";
        }
        if (timeout == null) {
            timeout = Duration.ofMinutes(30);
        }
    }
}
