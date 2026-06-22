package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Centraliza propriedades da integração OpenAI usada pela etapa de análise comercial. */
@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String apiKey,
        String baseUrl,
        String model,
        long requestTimeoutMs,
        String apiKeyFile
) {
    private static final Logger LOGGER = Logger.getLogger(OpenAiProperties.class.getName());

    /** Resolve a chave OpenAI a partir da variável direta ou do arquivo de secret configurado. */
    public String resolvedApiKey() {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        if (apiKeyFile == null || apiKeyFile.isBlank()) {
            return "";
        }
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of(apiKeyFile)).trim();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Falha ao ler arquivo de chave OpenAI na etapa pageanalysis. operacao=resolvedApiKey, apiKeyFile=" + apiKeyFile, ex);
            return "";
        }
    }

    /** Normaliza a URL base da API OpenAI removendo barra final e aplicando padrão oficial. */
    public String normalizedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** Normaliza o modelo OpenAI usado na análise comercial. */
    public String normalizedModel() {
        return (model == null || model.isBlank()) ? "gpt-5.2" : model.trim();
    }

    /** Normaliza o timeout máximo da chamada Responses Flex da OpenAI. */
    public long normalizedRequestTimeoutMs() {
        return requestTimeoutMs <= 0 ? 900000 : requestTimeoutMs;
    }
}
