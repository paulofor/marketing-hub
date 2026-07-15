package com.marketinghub.feo.infrastructure.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

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
        String openaiApiKeyFile,
        String imageModel,
        String imageQuality,
        boolean visualAssetsEnabled) {

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
        return StringUtils.hasText(resolvedOpenAiApiKey());
    }

    /**
     * Resolve a chave da OpenAI priorizando variável direta e aceitando arquivo secreto montado no container.
     */
    public String resolvedOpenAiApiKey() {
        if (StringUtils.hasText(openaiApiKey)) {
            return openaiApiKey.trim();
        }
        if (!StringUtils.hasText(openaiApiKeyFile)) {
            return "";
        }
        try {
            return Files.readString(Path.of(openaiApiKeyFile.trim())).trim();
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler arquivo secreto da OpenAI para o FEO", ex);
        }
    }
}
