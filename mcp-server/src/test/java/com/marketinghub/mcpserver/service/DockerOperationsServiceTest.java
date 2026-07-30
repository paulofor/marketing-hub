package com.marketinghub.mcpserver.service;

import com.marketinghub.mcpserver.config.McpProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Valida operações Docker restritas expostas pelo MCP para diagnóstico operacional.
 */
class DockerOperationsServiceTest {

    @TempDir
    private Path tempDir;

    /**
     * Garante que a listagem Docker retorna containers em estrutura auditável.
     */
    @Test
    void shouldListContainersWithAllowedFlag() throws Exception {
        DockerOperationsService service = new DockerOperationsService(buildProperties(fakeDockerCommand(), false));

        Map<String, Object> result = service.execute("ps", null, null, null);

        assertEquals("ps", result.get("action"));
        assertEquals(1, result.get("returnedContainers"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> containers = (List<Map<String, Object>>) result.get("containers");
        assertEquals("marketinghub-backend", containers.get(0).get("name"));
        assertEquals(true, containers.get(0).get("allowed"));
    }

    /**
     * Garante leitura de logs somente para container permitido e com filtro textual.
     */
    @Test
    void shouldReadDockerLogsForAllowedContainer() throws Exception {
        DockerOperationsService service = new DockerOperationsService(buildProperties(fakeDockerCommand(), false));

        Map<String, Object> result = service.execute("logs", "marketinghub-backend", 20, "Started");

        assertEquals("logs", result.get("action"));
        assertEquals("marketinghub-backend", result.get("container"));
        assertEquals(1, result.get("returnedLines"));
        assertEquals(List.of("2026-07-30T10:00:01Z Started AdsServiceApplication"), result.get("lines"));
    }

    /**
     * Garante que container fora da allowlist não pode ser operado pela tool.
     */
    @Test
    void shouldRejectContainerOutsideAllowList() throws Exception {
        DockerOperationsService service = new DockerOperationsService(buildProperties(fakeDockerCommand(), false));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.execute("logs", "mysql", 10, null));

        assertEquals("container must be one of: marketinghub-backend, product-discovery-worker",
                exception.getMessage());
    }

    /**
     * Garante que restart só roda quando a configuração explícita liberar a ação.
     */
    @Test
    void shouldRejectRestartWhenDisabled() throws Exception {
        DockerOperationsService service = new DockerOperationsService(buildProperties(fakeDockerCommand(), false));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.execute("restart", "marketinghub-backend", null, null));

        assertEquals("docker restart is disabled (set mcp.docker-ops.restart-enabled=true)",
                exception.getMessage());
    }

    /**
     * Garante que restart executa o Docker CLI quando está habilitado.
     */
    @Test
    void shouldRestartAllowedContainerWhenEnabled() throws Exception {
        DockerOperationsService service = new DockerOperationsService(buildProperties(fakeDockerCommand(), true));

        Map<String, Object> result = service.execute("restart", "marketinghub-backend", null, null);

        assertEquals("restart", result.get("action"));
        assertEquals("marketinghub-backend", result.get("container"));
        assertEquals("executed", result.get("status"));
        assertEquals(List.of("marketinghub-backend"), result.get("lines"));
    }

    /**
     * Cria um executável falso para simular o Docker CLI durante os testes.
     */
    private String fakeDockerCommand() throws Exception {
        Path script = tempDir.resolve("docker-fake.sh");
        Files.writeString(script, """
                #!/usr/bin/env sh
                if [ "$1" = "ps" ]; then
                  echo "marketinghub-backend|Up 2 minutes (healthy)|ghcr.io/acme/backend:sha"
                  echo "mysql|Up 4 days|mysql:5.7"
                  exit 0
                fi
                if [ "$1" = "logs" ]; then
                  echo "2026-07-30T10:00:00Z Liquibase migration completed"
                  echo "2026-07-30T10:00:01Z Started AdsServiceApplication"
                  exit 0
                fi
                if [ "$1" = "restart" ]; then
                  echo "$2"
                  exit 0
                fi
                echo "unsupported command"
                exit 1
                """, StandardCharsets.UTF_8);
        script.toFile().setExecutable(true);
        return script.toString();
    }

    /**
     * Monta propriedades MCP mínimas para testar operações Docker.
     */
    private McpProperties buildProperties(String dockerCommand, boolean restartEnabled) {
        McpProperties.Logs logs = new McpProperties.Logs(
                "/tmp/backend.log",
                "/tmp/ai-worker.log",
                "/tmp/lead-portal.log",
                "/tmp/facebook-ads.log",
                "/tmp/email-service.log",
                "/tmp/lead-portal-payment.log",
                "/tmp/mds.log",
                "/tmp/mois.log",
                "/tmp/mois-sales-library-worker.log",
                "/tmp/mois-hotmart.log",
                "/tmp/clickbank-coletor-mois.log",
                "/tmp/oprm-coletor-receita.log",
                "/tmp/ops-monitor-worker.log",
                "/tmp/pde-platform-backend.log",
                "/tmp/video-management-service.log",
                2,
                3,
                1,
                500,
                1024
        );
        McpProperties.ChatLogs chatLogs = new McpProperties.ChatLogs(
                true,
                List.of("marketinghub-fashion-chat"),
                dockerCommand,
                500,
                5
        );
        McpProperties.DockerOps dockerOps = new McpProperties.DockerOps(
                true,
                List.of("marketinghub-backend", "product-discovery-worker"),
                dockerCommand,
                500,
                5,
                restartEnabled
        );
        McpProperties.ProductDiscoveryWorker productDiscoveryWorker = new McpProperties.ProductDiscoveryWorker(
                true,
                "product-discovery-worker",
                dockerCommand,
                "http://127.0.0.1:8080/healthz",
                5
        );
        McpProperties.VpsHostInventory vpsHostInventory = new McpProperties.VpsHostInventory(
                false,
                List.of("191.252.210.83"),
                "ssh",
                "root",
                "/tmp/id_ed25519",
                "/tmp/known_hosts",
                5
        );
        McpProperties.Meta meta = new McpProperties.Meta(
                true,
                "https://graph.facebook.com",
                "v23.0",
                "",
                "",
                List.of("developers.facebook.com")
        );
        McpProperties.Github github = new McpProperties.Github(
                false,
                "https://api.github.com",
                "owner",
                "repo",
                ""
        );
        return new McpProperties("marketing-hub-mcp", "1.0.0", logs, chatLogs, dockerOps,
                vpsHostInventory, productDiscoveryWorker, meta, github);
    }
}
