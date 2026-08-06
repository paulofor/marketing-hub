package com.marketinghub.mcpserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mcpserver.config.McpProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Valida a consulta restrita do health do Product Discovery Worker pelo MCP.
 */
class ProductDiscoveryWorkerHealthServiceTest {

    @TempDir
    private Path tempDir;

    /**
     * Garante que o serviço executa o comando Docker fixo e retorna o payload de health estruturado.
     */
    @Test
    void shouldReadProductDiscoveryWorkerHealth() throws Exception {
        ProductDiscoveryWorkerHealthService service = new ProductDiscoveryWorkerHealthService(
                buildProperties(fakeDockerCommand()),
                new ObjectMapper()
        );

        Map<String, Object> result = service.readHealth();

        assertEquals("product-discovery-worker", result.get("container"));
        assertEquals(true, result.get("reachable"));
        Map<?, ?> payload = (Map<?, ?>) result.get("payload");
        assertEquals("UP", payload.get("status"));
        assertEquals("brave", payload.get("activeSearchProvider"));
        Map<?, ?> polling = (Map<?, ?>) payload.get("polling");
        assertEquals("COMPLETED", polling.get("lastPollStatus"));
    }

    /**
     * Cria um executável falso para simular docker exec durante o teste.
     */
    private String fakeDockerCommand() throws Exception {
        Path script = tempDir.resolve("docker-fake.sh");
        Files.writeString(script, """
                #!/usr/bin/env sh
                echo '{"service":"product-discovery-worker","status":"UP","activeSearchProvider":"brave","braveSearch":{"keyStatus":"CONFIGURED","keySource":"file"},"polling":{"lastPollStatus":"COMPLETED","lastPollError":null},"lastCycleProcessed":null}'
                """, StandardCharsets.UTF_8);
        script.toFile().setExecutable(true);
        return script.toString();
    }

    /**
     * Monta propriedades MCP mínimas para testar o health do Product Discovery Worker.
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
                "/tmp/experiment-strategist-worker.log",
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
                List.of("marketinghub-backend", "product-discovery-worker"),
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
