package com.marketinghub.nichocnae.meiaudiencesegmenter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Centraliza as configurações da OpenAI usadas exclusivamente pela segmentação comportamental MEI/autônomo. */
@ConfigurationProperties(prefix = "oprm.nichocnae.mei-audience-segmenter.openai")
public record MeiAudienceSegmenterOpenAiProperties(
        String baseUrl,
        String apiKey,
        String apiKeyFile,
        String model) {

    /** Normaliza valores padrão seguros para chamada síncrona da Responses API. */
    public MeiAudienceSegmenterOpenAiProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl;
        apiKey = apiKey == null ? "" : apiKey;
        apiKeyFile = apiKeyFile == null ? "" : apiKeyFile;
        model = model == null || model.isBlank() ? "gpt-4.1-mini" : model;
    }
}
