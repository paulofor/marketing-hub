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
 * Valida a leitura restrita de logs Docker dos containers operacionais pelo MCP.
 */
class ChatContainerLogServiceTest {

    @TempDir
    private Path tempDir;

    /**
     * Garante que apenas containers permitidos são aceitos para consulta de logs.
     */
    @Test
    void shouldRejectContainerOutsideAllowList() throws Exception {
        ChatContainerLogService service = new ChatContainerLogService(buildProperties(fakeDockerCommand()));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.readLogs("marketinghub-backend", 10, null));

        assertEquals("container must be one of: marketinghub-fashion-chat, product-discovery-worker",
                exception.getMessage());
    }

    /**
     * Garante execução do Docker CLI configurado e filtro textual sobre as linhas retornadas.
     */
    @Test
    void shouldReadAllowedContainerLogsAndFilterByText() throws Exception {
        ChatContainerLogService service = new ChatContainerLogService(buildProperties(fakeDockerCommand()));

        Map<String, Object> result = service.readLogs("marketinghub-fashion-chat", 20, "Fashion");

        assertEquals("marketinghub-fashion-chat", result.get("container"));
        assertEquals(1, result.get("returnedLines"));
        assertEquals(List.of("2026-07-12T10:00:00Z Fashion chat service listening on port 8094"),
                result.get("lines"));
    }

    /**
     * Cria um executável falso para simular o comando docker logs durante o teste.
     */
    private String fakeDockerCommand() throws Exception {
        Path script = tempDir.resolve("docker-fake.sh");
        Files.writeString(script, """
                #!/usr/bin/env sh
                echo "2026-07-12T10:00:00Z Fashion chat service listening on port 8094"
                echo "2026-07-12T10:00:01Z health ok"
                """, StandardCharsets.UTF_8);
        script.toFile().setExecutable(true);
        return script.toString();
    }

    /**
     * Monta propriedades MCP mínimas para testar logs de chat.
     */
    private McpProperties buildProperties(String dockerCommand) {
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
                "/tmp/customer-agent-worker.log",
                "/tmp/financial-agent-worker.log",
                2,
                3,
                1,
                500,
                1024
        );
        McpProperties.ChatLogs chatLogs = new McpProperties.ChatLogs(
                true,
                List.of("marketinghub-fashion-chat", "product-discovery-worker"),
                dockerCommand,
                500,
                5
        );
        McpProperties.ProductDiscoveryWorker productDiscoveryWorker = new McpProperties.ProductDiscoveryWorker(
                true,
                "product-discovery-worker",
                dockerCommand,
                "http://127.0.0.1:8080/healthz",
                5
        );
        McpProperties.DockerOps dockerOps = new McpProperties.DockerOps(
                true,
                List.of("marketinghub-backend", "marketinghub-fashion-chat", "product-discovery-worker"),
                dockerCommand,
                500,
                5,
                false
        );
        McpProperties.BuildInfo buildInfo = new McpProperties.BuildInfo(
                true,
                List.of("pde-platform-backend"),
                Map.of("pde-platform-backend", "http://127.0.0.1:8096/actuator/info"),
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
                buildInfo, vpsHostInventory, productDiscoveryWorker, meta, github);
    }
}
