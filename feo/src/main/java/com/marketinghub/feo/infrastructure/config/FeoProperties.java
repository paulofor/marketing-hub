package com.marketinghub.feo.infrastructure.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
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

    private static final String CANONICAL_IMAGE_MODEL = "gpt-image-2.5-sunburst";
    private static final String DEFAULT_IMAGE_QUALITY = "high";
    private static final Set<String> SUPPORTED_IMAGE_QUALITIES =
            Set.of("low", "medium", "high", "xhigh", "max", "auto");

    /** Normaliza modelo e qualidade para impedir downgrade silencioso em novas execuções. */
    public FeoProperties {
        imageModel = CANONICAL_IMAGE_MODEL;
        String normalizedQuality = StringUtils.hasText(imageQuality)
                ? imageQuality.trim().toLowerCase(Locale.ROOT)
                : DEFAULT_IMAGE_QUALITY;
        imageQuality = SUPPORTED_IMAGE_QUALITIES.contains(normalizedQuality)
                ? normalizedQuality
                : DEFAULT_IMAGE_QUALITY;
    }

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
