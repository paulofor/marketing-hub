package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String apiKey,
        String baseUrl,
        String model,
        long batchPollIntervalMs,
        long batchTimeoutMs,
        String apiKeyFile
) {

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
            return "";
        }
    }

    public String normalizedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String normalizedModel() {
        return (model == null || model.isBlank()) ? "gpt-5.2" : model.trim();
    }

    public long normalizedBatchPollIntervalMs() {
        return batchPollIntervalMs <= 0 ? 2000 : batchPollIntervalMs;
    }

    public long normalizedBatchTimeoutMs() {
        return batchTimeoutMs <= 0 ? 300000 : batchTimeoutMs;
    }
}
