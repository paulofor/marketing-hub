package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker")
public record WorkerProperties(
        String backendBaseUrl,
        String workspaceId,
        String source,
        String sources,
        long pollIntervalMs,
        int requestTimeoutMs) {}
