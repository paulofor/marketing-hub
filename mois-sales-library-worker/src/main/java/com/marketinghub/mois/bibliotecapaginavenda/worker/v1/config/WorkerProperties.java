package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centraliza parâmetros operacionais do worker MOIS de Biblioteca de Páginas de Vendas.
 */
@ConfigurationProperties(prefix = "worker")
public record WorkerProperties(
        String backendBaseUrl,
        String workspaceId,
        String source,
        String sources,
        String rawHtmlSource,
        String rawHtmlSources,
        long pollIntervalMs,
        long rawHtmlPollIntervalMs,
        int requestTimeoutMs) {}
