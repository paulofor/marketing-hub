package com.marketinghub.videomanagement.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: criar e renovar a sessão Codex individual de Apolo. */
@Component
public class ApolloCodexAuthReconnectCoordinator {
    private static final Logger log = LoggerFactory.getLogger(ApolloCodexAuthReconnectCoordinator.class);
    private final RestClient backend;
    private final String backendUrl;
    private final String repositoryPath;
    private final String agentKey;

    /** Configura o backend, o script de autenticação e a identidade técnica de Apolo. */
    public ApolloCodexAuthReconnectCoordinator(
            @Value("${apollo.codex-auth.backend-url}") String backendUrl,
            @Value("${apollo.codex-auth.repository-path}") String repositoryPath,
            @Value("${apollo.codex-auth.agent-key}") String agentKey) {
        this.backend = RestClient.builder().baseUrl(backendUrl).build();
        this.backendUrl = backendUrl;
        this.repositoryPath = repositoryPath;
        this.agentKey = agentKey;
    }

    /** Consulta a fila canônica e executa somente a reconexão destinada a Apolo. */
    @Scheduled(cron = "5/15 * * * * *")
    public void processPending() {
        try {
            ResponseEntity<ReconnectJob> response = backend.get()
                    .uri("/api/internal/agents/executor-health/{agentKey}/codex-auth/reconnections/pending", agentKey)
                    .retrieve().toEntity(ReconnectJob.class);
            if (response.getBody() != null) execute(response.getBody());
        } catch (RuntimeException ex) {
            log.error("Falha ao consultar reconexão Codex de Apolo. agentKey={}", agentKey, ex);
        }
    }

    /** Executa o device code no CODEX_HOME exclusivo de Apolo. */
    void execute(ReconnectJob job) {
        ProcessBuilder builder = new ProcessBuilder("node", Path.of(repositoryPath)
                .resolve("scripts/codex-app-server-device-login.mjs").toString());
        builder.inheritIO();
        builder.environment().put("CODEX_AUTH_RECONNECT_ID", job.id().toString());
        builder.environment().put("CODEX_AUTH_CALLBACK_BASE_URL", backendUrl);
        try {
            Process process = builder.start();
            if (!process.waitFor(16, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                complete(job.id(), "Tempo esgotado aguardando confirmação.");
            } else if (process.exitValue() != 0) {
                complete(job.id(), "Codex App Server encerrou com falha.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Reconexão Codex de Apolo interrompida. reconnectId={}", job.id(), ex);
            complete(job.id(), "Reconexão interrompida.");
        } catch (IOException ex) {
            log.error("Falha ao iniciar App Server de Apolo. reconnectId={}", job.id(), ex);
            complete(job.id(), "Não foi possível iniciar o App Server.");
        }
    }

    /** Registra falha local sem transportar credenciais. */
    private void complete(Long id, String detail) {
        try {
            backend.post()
                    .uri("/api/internal/agents/executor-health/codex-auth/reconnections/{id}/completion", id)
                    .body(Map.of("authenticated", false, "detail", detail))
                    .retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            log.error("Falha ao registrar reconexão Codex de Apolo. reconnectId={}", id, ex);
        }
    }

    /** Representa o identificador seguro do pedido de reconexão. */
    record ReconnectJob(Long id) {}
}
