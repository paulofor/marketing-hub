package com.marketinghub.opsmonitor.pipeline.logscan;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Envia incidentes detectados em logs para o backend principal. */
public class LogScanBackendClient {
    private final WebClient webClient;

    /** Recebe o cliente HTTP do backend. */
    public LogScanBackendClient(WebClient webClient) { this.webClient = webClient; }

    /** Registra incidente quando a etapa encontra sinais relevantes. */
    public Mono<Void> sendIncident(LogScanOutput output) {
        return webClient.post().uri("/api/internal/ops-monitor/v1/modules/{moduleCode}/incidents", output.moduleCode())
                .bodyValue(output).retrieve().bodyToMono(Void.class);
    }
}
