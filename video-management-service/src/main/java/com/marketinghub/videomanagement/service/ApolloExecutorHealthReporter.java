package com.marketinghub.videomanagement.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: publicar periodicamente a prontidão real do executor Codex de Apolo. */
@Component
public class ApolloExecutorHealthReporter {
    private static final Logger log = LoggerFactory.getLogger(ApolloExecutorHealthReporter.class);
    private final RestClient backend;
    private final String agentKey;
    private final int deployedVersion;
    private final String buildReference;

    /** Configura o contrato canônico de telemetria e a identidade implantada de Apolo. */
    public ApolloExecutorHealthReporter(
            @Value("${apollo.codex-auth.backend-url}") String backendUrl,
            @Value("${apollo.codex-auth.agent-key}") String agentKey,
            @Value("${apollo.executor-health.deployed-version}") int deployedVersion,
            @Value("${apollo.executor-health.build-reference}") String buildReference) {
        this.backend = RestClient.builder().baseUrl(backendUrl).build();
        this.agentKey = agentKey;
        this.deployedVersion = deployedVersion;
        this.buildReference = buildReference;
    }

    /** Comprova acesso ao backend e autenticação Codex sem expor credenciais. */
    @Scheduled(cron = "10 * * * * *")
    public void report() {
        boolean authenticated = codexAuthenticated();
        try {
            backend.post()
                    .uri("/api/internal/agents/executor-health")
                    .body(Map.of(
                            "agentKey", agentKey,
                            "deployedVersion", deployedVersion,
                            "buildReference", buildReference,
                            "backendAccessible", true,
                            "codexAuthenticated", authenticated,
                            "detail", authenticated ? "Executor Apolo pronto." : "Reconecte a sessão Codex individual de Apolo."))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.error("Falha ao publicar health-check de Apolo. agentKey={}", agentKey, ex);
        }
    }

    /** Valida localmente a sessão individual com tempo máximo controlado. */
    boolean codexAuthenticated() {
        try {
            Process process = new ProcessBuilder("codex", "login", "status").redirectErrorStream(true).start();
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Health-check Codex de Apolo interrompido. agentKey={}", agentKey, ex);
            return false;
        } catch (IOException ex) {
            log.error("Falha ao validar sessão Codex de Apolo. agentKey={}", agentKey, ex);
            return false;
        }
    }
}
