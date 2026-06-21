package com.marketinghub.nichocnae.meiaudiencesegmenter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Centraliza as configurações da OpenAI usadas exclusivamente pela segmentação comportamental MEI/autônomo. */
@ConfigurationProperties(prefix = "oprm.nichocnae.mei-audience-segmenter.openai")
public record MeiAudienceSegmenterOpenAiProperties(
        String baseUrl,
        String apiKey,
        String apiKeyFile,
        String model,
        String expectedApiKeyVariable,
        String fallbackApiKeyVariable,
        String serviceTier) {

    /** Normaliza valores padrão seguros para chamada síncrona da Responses API. */
    public MeiAudienceSegmenterOpenAiProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl;
        apiKey = apiKey == null ? "" : apiKey;
        apiKeyFile = apiKeyFile == null ? "" : apiKeyFile;
        model = model == null || model.isBlank() ? "gpt-5.2" : model;
        expectedApiKeyVariable = expectedApiKeyVariable == null || expectedApiKeyVariable.isBlank()
                ? "OPRM_MEI_AUDIENCE_SEGMENTER_OPENAI_API_KEY"
                : expectedApiKeyVariable;
        fallbackApiKeyVariable = fallbackApiKeyVariable == null || fallbackApiKeyVariable.isBlank()
                ? "OPENAI_API_KEY"
                : fallbackApiKeyVariable;
        serviceTier = serviceTier == null || serviceTier.isBlank() ? "flex" : serviceTier;
    }
}
