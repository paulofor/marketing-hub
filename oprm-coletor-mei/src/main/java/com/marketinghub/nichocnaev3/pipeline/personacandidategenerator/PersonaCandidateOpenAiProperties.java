package com.marketinghub.nichocnaev3.pipeline.personacandidategenerator;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Centraliza as configurações OpenAI da etapa persona-candidate-generator do NichoCNAE v3. */
@ConfigurationProperties(prefix = "oprm.nichocnaev3.persona-candidate-generator.openai")
public record PersonaCandidateOpenAiProperties(
        String baseUrl,
        String apiKey,
        String apiKeyFile,
        String model,
        String serviceTier) {

    /** Normaliza padrões operacionais para Responses API em modo Flex. */
    public PersonaCandidateOpenAiProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl;
        apiKey = apiKey == null ? "" : apiKey;
        apiKeyFile = apiKeyFile == null ? "" : apiKeyFile;
        model = model == null || model.isBlank() ? "gpt-5.2" : model;
        serviceTier = serviceTier == null || serviceTier.isBlank() ? "flex" : serviceTier;
    }
}
