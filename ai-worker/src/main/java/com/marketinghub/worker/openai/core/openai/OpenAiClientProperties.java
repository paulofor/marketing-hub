package com.marketinghub.worker.openai.core.openai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;

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

        BigDecimal inputUsdPerMillionTokens,

        BigDecimal outputUsdPerMillionTokens
) {
    public OpenAiClientProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1";
        }
        if (timeout == null) {
            timeout = Duration.ofMinutes(30);
        }
        if (inputUsdPerMillionTokens == null) {
            inputUsdPerMillionTokens = BigDecimal.ZERO;
        }
        if (outputUsdPerMillionTokens == null) {
            outputUsdPerMillionTokens = BigDecimal.ZERO;
        }
    }
}
