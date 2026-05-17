package com.marketinghub.mois.libraryworker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker")
public record WorkerProperties(String backendBaseUrl, String workspaceId, String source, long pollIntervalMs, int requestTimeoutMs) {}
