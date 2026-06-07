package com.marketinghub.worker.openai.core.openai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Responsabilidade: centralizar credenciais, modelo, catálogo de preços e URL efetiva dos clientes OpenAI. */
@Validated
@ConfigurationProperties(prefix = "openai")
public record OpenAiClientProperties(
        @NotBlank
        String apiKey,

        @NotBlank
        String baseUrl,

        @NotBlank
        String model,

        @NotNull
        Duration timeout,

        @NotBlank
        String pricingCatalogUrl,

        boolean allowLocalBaseUrl
) {
    /** Normaliza valores opcionais e bloqueia base URL local salvo permissão explícita. */
    public OpenAiClientProperties {
        baseUrl = OpenAiBaseUrlGuard.resolve(baseUrl, allowLocalBaseUrl);
        if (timeout == null) {
            timeout = Duration.ofMinutes(30);
        }
        if (pricingCatalogUrl == null || pricingCatalogUrl.isBlank()) {
            pricingCatalogUrl = "http://191.252.181.168/api/modelos/openai/catalogo/v1/modelos";
        }
    }
}
