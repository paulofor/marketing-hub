package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Centraliza as configurações da OpenAI usadas exclusivamente pela etapa dois de seed do nicho CNAE. */
@ConfigurationProperties(prefix = "oprm.nichocnae.seed-builder.openai")
public record NicheResearchSeedBuilderOpenAiProperties(
        String baseUrl,
        String apiKey,
        String apiKeyFile,
        String model,
        String serviceTier) {

    /** Normaliza valores padrão seguros para a chamada síncrona da Responses API. */
    public NicheResearchSeedBuilderOpenAiProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl;
        apiKey = apiKey == null ? "" : apiKey;
        apiKeyFile = apiKeyFile == null ? "" : apiKeyFile;
        model = model == null || model.isBlank() ? "gpt-4.1-mini" : model;
        serviceTier = serviceTier == null || serviceTier.isBlank() ? "flex" : serviceTier;
    }
}
