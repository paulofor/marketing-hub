package com.marketinghub.worker.openai.core.copy;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Responsabilidade: concentrar as propriedades operacionais do worker OpenAI da etapa copy. */
@Validated
@ConfigurationProperties(prefix = "copy.worker")
public record CopyWorkerProperties(
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
        Duration timeout
) {
    /** Normaliza valores opcionais usados pelo worker de copy quando a configuração externa omite o campo. */
    public CopyWorkerProperties {
        if (apiPrefix == null || apiPrefix.isBlank()) {
            apiPrefix = "/api";
        }
        if (timeout == null) {
            timeout = Duration.ofMinutes(30);
        }
    }
}
