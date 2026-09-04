package com.marketinghub.harnesslibraryapi.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Reúne endpoints, tempos limite e referências de secrets do gateway. */
@ConfigurationProperties(prefix = "harness-library")
public record HarnessLibraryProperties(
    String apiKey,
    String apiKeyFile,
    String backendBaseUrl,
    String internalSigningKey,
    String internalSigningKeyFile,
    Duration connectTimeout,
    Duration readTimeout) {}
