package com.marketinghub.pipelines.geracaoanuncios.v1.imagem;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Responsabilidade: concentrar as propriedades operacionais da etapa Imagem do GeracaoAnuncios v1. */
@Validated
@ConfigurationProperties(prefix = "geracaoanuncios.v1.imagem.worker")
public record GeraAnuncioImagemWorkerProperties(
        boolean enabled,
        @Min(1) int pendingLimit,
        @NotBlank String backendBaseUrl,
        String apiPrefix,
        Duration timeout) {
    /** Normaliza valores opcionais para manter o worker operacional sem configuração extra em desenvolvimento. */
    public GeraAnuncioImagemWorkerProperties {
        if (pendingLimit < 1) {
            pendingLimit = 10;
        }
        if (backendBaseUrl == null || backendBaseUrl.isBlank()) {
            backendBaseUrl = "http://191.252.181.168:8000";
        }
        if (apiPrefix == null || apiPrefix.isBlank()) {
            apiPrefix = "/api";
        }
        if (timeout == null) {
            timeout = Duration.ofMinutes(2);
        }
    }
}
