package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Centraliza as configurações OpenAI da qualificação de fontes do source-searcher. */
@ConfigurationProperties(prefix = "oprm.pipelines.nichocnae.v3.source-searcher.openai")
public record SourceSearcherOpenAiProperties(
        String baseUrl,
        String apiKey,
        String apiKeyFile,
        String model,
        String serviceTier,
        Boolean enabled) {

    /** Normaliza padrões operacionais da Responses API para qualificação auditável. */
    public SourceSearcherOpenAiProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl;
        apiKey = apiKey == null ? "" : apiKey;
        apiKeyFile = apiKeyFile == null ? "" : apiKeyFile;
        model = model == null || model.isBlank() ? "gpt-5.2" : model;
        serviceTier = serviceTier == null || serviceTier.isBlank() ? "flex" : serviceTier;
        enabled = enabled != null && enabled;
    }
}
