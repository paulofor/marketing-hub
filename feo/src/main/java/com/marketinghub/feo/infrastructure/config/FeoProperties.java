package com.marketinghub.feo.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centraliza as configuracoes operacionais do worker FEO.
 */
@ConfigurationProperties(prefix = "feo")
public record FeoProperties(
        String workerId,
        String backendBaseUrl,
        int pendingLimit,
        String outputDir,
        String openaiBaseUrl,
        String openaiApiKey,
        String imageModel,
        String imageQuality) {

    /**
     * Retorna o limite de pendencias protegido contra valores invalidos.
     */
    public int safePendingLimit() {
        return pendingLimit <= 0 ? 1 : pendingLimit;
    }

    /**
     * Indica se a geração de imagens pode chamar a OpenAI.
     */
    public boolean hasOpenAiApiKey() {
        return openaiApiKey != null && !openaiApiKey.isBlank();
    }
}
