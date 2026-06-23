package com.marketinghub.opsmonitor.pipeline.healthcheck;

import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Envia ao backend o heartbeat produzido pela etapa de health check. */
public class HealthCheckBackendClient {
    private final WebClient webClient;

    /** Recebe o cliente HTTP apontado para o backend principal. */
    public HealthCheckBackendClient(WebClient webClient) { this.webClient = webClient; }

    /** Busca no backend a lista canônica de módulos pendentes de verificação. */
    public List<PendingModuleCheck> fetchPendingChecks() {
        return webClient.get().uri("/api/internal/ops-monitor/v1/module-checks/stage-executions/pending")
                .retrieve().bodyToMono(new ParameterizedTypeReference<List<PendingModuleCheck>>() {})
                .blockOptional().orElse(List.of());
    }

    /** Registra o heartbeat do módulo monitorado no contrato oficial do backend. */
    public Mono<Void> sendHeartbeat(HealthCheckOutput output) {
        return webClient.post().uri("/api/internal/ops-monitor/v1/modules/{moduleCode}/heartbeat", output.moduleCode())
                .bodyValue(output).retrieve().bodyToMono(Void.class);
    }
}
