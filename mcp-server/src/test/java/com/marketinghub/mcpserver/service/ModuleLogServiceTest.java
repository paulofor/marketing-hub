package com.marketinghub.mcpserver.service;

import com.marketinghub.mcpserver.config.McpProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Valida a leitura resiliente de logs dos módulos Java pelo servidor MCP.
 */
class ModuleLogServiceTest {

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
     * Garante retentativas em falha temporária e retorno apenas das últimas linhas solicitadas.
     */
    @Test
    void shouldRetryWhenEndpointFailsAndReturnTailLines() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/logs", exchange -> {
            int call = calls.incrementAndGet();
            if (call < 3) {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
                return;
            }
            byte[] body = "line-1\nline-2\nline-3\n".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();

        String url = "http://localhost:" + server.getAddress().getPort() + "/logs";
        ModuleLogService service = new ModuleLogService(buildProperties(url));

        Map<String, Object> result = service.readModuleLogs("backend", 2, null, null, null, null, null);

        assertEquals(3, calls.get());
        assertEquals(2, result.get("returnedLines"));
        assertEquals(List.of("line-2", "line-3"), result.get("lines"));
    }

    /**
     * Garante filtro estruturado por erro HTTP, endpoint e requestId.
     */
    @Test
    void shouldFilterByHttpStatusEndpointAndRequestId() throws Exception {
        Path logPath = Path.of("target/test-logs/backend.log");
        Files.createDirectories(logPath.getParent());
        Files.writeString(logPath,
                "2026-07-25T21:07:00Z ERROR Erro HTTP 500 não tratado. requestId=req-500-creative status=500 method=POST endpoint=/api/creatives/10/reject\n"
                        + "2026-07-25T21:08:00Z ERROR Erro HTTP 500 não tratado. requestId=req-500-other status=500 method=POST endpoint=/api/other\n",
                StandardCharsets.UTF_8);

        ModuleLogService service = new ModuleLogService(buildProperties(logPath.toString()));

        Map<String, Object> result = service.readModuleLogs(
                "backend",
                10,
                null,
                500,
                "/api/creatives/10/reject",
                "req-500-creative",
                null,
                null,
                null,
                null);

        assertEquals(1, result.get("returnedLines"));
        assertEquals(List.of("2026-07-25T21:07:00Z ERROR Erro HTTP 500 não tratado. requestId=req-500-creative status=500 method=POST endpoint=/api/creatives/10/reject"), result.get("lines"));
    }

    /**
     * Garante que endpoints HTTP agregados com JSON em lines sejam tratados como linhas de log reais.
     */
    @Test
    void shouldExtractLinesFromJsonLogEndpoint() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/runtime-logs/tail", exchange -> {
            byte[] body = """
                    {"available":true,"errorMessage":null,"lines":["2026-07-26T04:05:48Z ERROR video failed","2026-07-26T04:07:56Z INFO recovered"],"generatedAt":"2026-07-26T04:08:00Z"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();

        String url = "http://localhost:" + server.getAddress().getPort() + "/runtime-logs/tail";
        ModuleLogService service = new ModuleLogService(buildProperties(url));

        Map<String, Object> result = service.readModuleLogs("facebook-ads", 10, "video failed", null, null, null, null);

        assertEquals(1, result.get("returnedLines"));
        assertEquals(List.of("2026-07-26T04:05:48Z ERROR video failed"), result.get("lines"));
    }

    /**
     * Monta propriedades MCP apontando todos os módulos para a URL local informada.
     */
    private McpProperties buildProperties(String logUrl) {
        McpProperties.Logs logs = new McpProperties.Logs(
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                logUrl,
                2,
                3,
                1,
                500,
                1024
        );

        McpProperties.Meta meta = new McpProperties.Meta(
                true,
                "https://graph.facebook.com",
                "v23.0",
                "",
                "",
                List.of("developers.facebook.com")
        );

        McpProperties.ChatLogs chatLogs = new McpProperties.ChatLogs(
                true,
                List.of("marketinghub-fashion-chat"),
                "docker",
                500,
                20
        );

        McpProperties.Github github = new McpProperties.Github(
                false,
                "https://api.github.com",
                "owner",
                "repo",
                ""
        );
        McpProperties.ProductDiscoveryWorker productDiscoveryWorker = new McpProperties.ProductDiscoveryWorker(
                true,
                "product-discovery-worker",
                "docker",
                "http://127.0.0.1:8080/healthz",
                10
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

        return new McpProperties("marketing-hub-mcp", "1.0.0", logs, chatLogs, dockerOps,
                buildInfo, vpsHostInventory, productDiscoveryWorker, meta, github);
    }
}
