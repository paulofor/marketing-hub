package com.marketinghub.mcpserver.service;

import com.marketinghub.mcpserver.config.McpProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida a consulta de identidade de build publicada por módulos em runtime.
 */
class RuntimeBuildInfoServiceTest {

    private HttpServer server;

    /**
     * Encerra o servidor HTTP local usado pelos testes.
     */
    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * Garante que campos de commit, branch, build time e versão são extraídos do Actuator.
     */
    @Test
    void shouldExtractBuildIdentityFromActuatorInfo() throws Exception {
        RuntimeBuildInfoService service = new RuntimeBuildInfoService(buildProperties("""
                {"git":{"branch":"main","commit":{"id":"abcdef123456","id.abbrev":"abcdef1"}},"build":{"version":"1.2.3","time":"2026-07-31T10:00:00Z"}}
                """));

        Map<String, Object> result = service.readBuildInfo("pde-platform-backend");

        assertTrue((Boolean) result.get("buildIdentityPublished"));
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertEquals("1.2.3", summary.get("version"));
        assertEquals("abcdef123456", summary.get("commitId"));
        assertEquals("main", summary.get("branch"));
        assertEquals("2026-07-31T10:00:00Z", summary.get("buildTime"));
    }

    /**
     * Garante que payload vazio não é tratado como confirmação de versão produtiva.
     */
    @Test
    void shouldFlagEmptyActuatorInfoAsMissingBuildIdentity() throws Exception {
        RuntimeBuildInfoService service = new RuntimeBuildInfoService(buildProperties("{}"));

        Map<String, Object> result = service.readBuildInfo("pde-platform-backend");

        assertFalse((Boolean) result.get("buildIdentityPublished"));
        assertEquals("O endpoint respondeu, mas não publicou commit, branch, build time ou version rastreável.",
                result.get("diagnostic"));
    }

    /**
     * Garante que o backend principal do Marketing Hub pode ser consultado pela mesma tool.
     */
    @Test
    void shouldReadMainBackendBuildIdentity() throws Exception {
        RuntimeBuildInfoService service = new RuntimeBuildInfoService(buildProperties("""
                {"git":{"branch":"main","commit":{"id":"123456789abc"}},"build":{"version":"2.0.0","time":"2026-07-31T12:00:00Z"}}
                """));

        Map<String, Object> result = service.readBuildInfo("backend");

        assertEquals("backend", result.get("module"));
        assertTrue((Boolean) result.get("buildIdentityPublished"));
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertEquals("123456789abc", summary.get("commitId"));
        assertEquals("main", summary.get("branch"));
    }

    /**
     * Monta propriedades MCP apontando o build info para um servidor HTTP local.
     */
    private McpProperties buildProperties(String actuatorBody) throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/actuator/info", exchange -> {
            byte[] body = actuatorBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();
        String infoUrl = "http://localhost:" + server.getAddress().getPort() + "/actuator/info";

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
                "docker",
                500,
                20
        );
        McpProperties.DockerOps dockerOps = new McpProperties.DockerOps(
                true,
                List.of("marketinghub-backend"),
                "docker",
                500,
                30,
                false
        );
        McpProperties.BuildInfo buildInfo = new McpProperties.BuildInfo(
                true,
                List.of("backend", "pde-platform-backend"),
                Map.of(
                        "backend", infoUrl,
                        "pde-platform-backend", infoUrl),
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
        McpProperties.ProductDiscoveryWorker productDiscoveryWorker = new McpProperties.ProductDiscoveryWorker(
                true,
                "product-discovery-worker",
                "docker",
                "http://127.0.0.1:8080/healthz",
                10
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
        return new McpProperties("marketing-hub-mcp", "1.0.0", logs, chatLogs, dockerOps, buildInfo,
                vpsHostInventory, productDiscoveryWorker, meta, github);
    }
}
