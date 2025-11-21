package com.marketinghub.worker.openai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiApiKeyProvider {
    private static final Logger log = LoggerFactory.getLogger(OpenAiApiKeyProvider.class);

    private final String apiKey;

    public OpenAiApiKeyProvider(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.api-key-file:}") String apiKeyFile) {
        this.apiKey = resolveApiKey(apiKey, apiKeyFile);
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private String resolveApiKey(String apiKey, String apiKeyFile) {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        if (apiKeyFile == null || apiKeyFile.isBlank()) {
            return "";
        }
        Path path = Paths.get(apiKeyFile);
        if (!Files.exists(path)) {
            log.warn("OpenAI API key file {} not found", path);
            return "";
        }
        try {
            String fromFile = Files.readString(path).trim();
            if (fromFile.isEmpty()) {
                log.warn("OpenAI API key file {} is empty", path);
            }
            return fromFile;
        } catch (IOException ex) {
            log.error("Failed to read OpenAI API key file {}", path, ex);
            return "";
        }
    }
}

