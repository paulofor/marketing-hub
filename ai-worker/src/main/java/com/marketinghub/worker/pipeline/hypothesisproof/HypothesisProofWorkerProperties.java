package com.marketinghub.worker.pipeline.hypothesisproof;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Responsabilidade: concentrar as propriedades operacionais da etapa Prova do pipeline de hipótese. */
@Validated
@ConfigurationProperties(prefix = "hypothesis-proof.worker")
public record HypothesisProofWorkerProperties(
        boolean enabled,
        @Min(1) int pendingLimit,
        @NotBlank String backendBaseUrl,
        String apiPrefix,
        @NotBlank String promptResource,
        @NotBlank String schemaResource,
        @NotBlank String schemaName,
        @NotBlank String model,
        @NotNull Duration timeout
) {
    /** Normaliza valores opcionais usados pelo worker quando a configuração externa omite o campo. */
    public HypothesisProofWorkerProperties {
        if (apiPrefix == null || apiPrefix.isBlank()) {
            apiPrefix = "/api";
        }
        if (model == null || model.isBlank()) {
            model = "gpt-5.5";
        }
        if (timeout == null) {
            timeout = Duration.ofMinutes(30);
        }
    }
}
