package com.marketinghub.mcpserver.service;

import com.marketinghub.mcpserver.config.McpProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleLogServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

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

        Map<String, Object> result = service.readModuleLogs("backend", 2);

        assertEquals(3, calls.get());
        assertEquals(2, result.get("returnedLines"));
        assertEquals(List.of("line-2", "line-3"), result.get("lines"));
    }

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

        McpProperties.Github github = new McpProperties.Github(
                false,
                "https://api.github.com",
                "owner",
                "repo",
                ""
        );

        return new McpProperties("marketing-hub-mcp", "1.0.0", "", logs, meta, github);
    }
}
