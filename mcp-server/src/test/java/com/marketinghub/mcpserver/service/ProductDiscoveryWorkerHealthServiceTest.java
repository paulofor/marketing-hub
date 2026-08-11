package com.marketinghub.mcpserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mcpserver.config.McpProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Valida a consulta restrita do health do Product Discovery Worker pelo MCP.
 */
class ProductDiscoveryWorkerHealthServiceTest {

    private HttpServer server;

    /** Encerra o servidor HTTP usado pelo teste. */
    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    /**
     * Garante que o serviço consulta o host real por HTTP e retorna o payload estruturado.
     */
    @Test
    void shouldReadProductDiscoveryWorkerHealth() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/healthz", exchange -> {
            byte[] body = "{\"service\":\"product-discovery-worker\",\"status\":\"UP\",\"activeSearchProvider\":\"brave\",\"polling\":{\"lastPollStatus\":\"COMPLETED\"}}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        String healthUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/healthz";
        ProductDiscoveryWorkerHealthService service = new ProductDiscoveryWorkerHealthService(
                buildProperties(healthUrl),
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
     * Monta propriedades MCP mínimas para testar o health do Product Discovery Worker.
     */
    private McpProperties buildProperties(String healthUrl) {
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
                "/tmp/meta-ad-approver-worker.log",
                2,
                3,
                1,
                500,
                1024
        );
        McpProperties.ChatLogs chatLogs = new McpProperties.ChatLogs(
                true,
                List.of("marketinghub-fashion-chat", "product-discovery-worker"),
                "docker",
                500,
                5
        );
        McpProperties.ProductDiscoveryWorker productDiscoveryWorker = new McpProperties.ProductDiscoveryWorker(
                true,
                "product-discovery-worker",
                "docker",
                healthUrl,
                5
        );
        McpProperties.DockerOps dockerOps = new McpProperties.DockerOps(
                true,
                List.of("marketinghub-backend", "product-discovery-worker"),
                "docker",
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
