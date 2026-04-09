package com.marketinghub.mcpserver.controller;

import com.marketinghub.mcpserver.config.McpProperties;
import com.marketinghub.mcpserver.service.DatabaseDiagnosticsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private final McpProperties properties;
    private final DatabaseDiagnosticsService databaseDiagnosticsService;

    public McpController(McpProperties properties, DatabaseDiagnosticsService databaseDiagnosticsService) {
        this.properties = properties;
        this.databaseDiagnosticsService = databaseDiagnosticsService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleRequest(@RequestBody Map<String, Object> request) {
        String method = String.valueOf(request.getOrDefault("method", ""));
        Object id = request.get("id");

        return switch (method) {
            case "initialize" -> ResponseEntity.ok(success(id, Map.of(
                    "protocolVersion", "2024-11-05",
                    "serverInfo", Map.of(
                            "name", properties.serverName(),
                            "version", properties.serverVersion()),
                    "capabilities", Map.of(
                            "tools", Map.of())
            )));
            case "tools/list" -> ResponseEntity.ok(success(id, Map.of("tools", List.of(
                    Map.of(
                            "name", "db_health",
                            "description", "Valida conectividade com o mesmo banco MySQL usado pelo backend.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(),
                                    "additionalProperties", false)
                    )))));
            case "tools/call" -> ResponseEntity.ok(callTool(id, request));
            default -> ResponseEntity.ok(error(id, -32601, "Method not found: " + method));
        };
    }

    private Map<String, Object> callTool(Object id, Map<String, Object> request) {
        Object paramsObject = request.get("params");
        if (!(paramsObject instanceof Map<?, ?> params)) {
            return error(id, -32602, "Invalid params");
        }

        String toolName = String.valueOf(params.getOrDefault("name", ""));
        if (!"db_health".equals(toolName)) {
            return error(id, -32602, "Unknown tool: " + toolName);
        }

        Map<String, Object> diagnostics = databaseDiagnosticsService.checkConnection();
        return success(id, Map.of(
                "content", List.of(Map.of(
                        "type", "text",
                        "text", "Database connectivity status: " + diagnostics.get("status")
                                + " (database=" + diagnostics.get("database") + ")")),
                "structuredContent", diagnostics
        ));
    }

    private Map<String, Object> success(Object id, Map<String, Object> result) {
        return Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "result", result
        );
    }

    private Map<String, Object> error(Object id, int code, String message) {
        return Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "error", Map.of(
                        "code", code,
                        "message", message)
        );
    }
}
