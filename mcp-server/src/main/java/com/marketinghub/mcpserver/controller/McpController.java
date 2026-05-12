package com.marketinghub.mcpserver.controller;

import com.marketinghub.mcpserver.config.McpProperties;
import com.marketinghub.mcpserver.service.DatabaseDiagnosticsService;
import com.marketinghub.mcpserver.service.MetaDiagnosticsService;
import com.marketinghub.mcpserver.service.GithubActionsService;
import com.marketinghub.mcpserver.service.ModuleLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int MAX_QUERY_LIMIT = 500;

    private final McpProperties properties;
    private final DatabaseDiagnosticsService databaseDiagnosticsService;
    private final ModuleLogService moduleLogService;
    private final MetaDiagnosticsService metaDiagnosticsService;
    private final GithubActionsService githubActionsService;

    public McpController(McpProperties properties,
                         DatabaseDiagnosticsService databaseDiagnosticsService,
                         ModuleLogService moduleLogService,
                         MetaDiagnosticsService metaDiagnosticsService,
                         GithubActionsService githubActionsService) {
        this.properties = properties;
        this.databaseDiagnosticsService = databaseDiagnosticsService;
        this.moduleLogService = moduleLogService;
        this.metaDiagnosticsService = metaDiagnosticsService;
        this.githubActionsService = githubActionsService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> describeEndpoint() {
        return ResponseEntity.ok(Map.of(
                "name", properties.serverName(),
                "version", properties.serverVersion(),
                "endpoint", "/mcp",
                "protocol", "json-rpc-2.0",
                "hint", "Use HTTP POST with JSON-RPC methods initialize, tools/list, tools/call"
        ));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleRequest(@RequestBody Map<String, Object> request,
                                                             HttpServletRequest httpServletRequest) {
        Object id = request.get("id");
        String method = String.valueOf(request.getOrDefault("method", ""));
        logger.info("Nova requisição MCP recebida: requestId={} method={} remoteAddr={} userAgent={}",
                id, method, httpServletRequest.getRemoteAddr(), httpServletRequest.getHeader("User-Agent"));
        if (!isAuthorized(httpServletRequest)) {
            return ResponseEntity.status(401).body(error(id, -32001, "Unauthorized"));
        }

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
                    ),
                    Map.of(
                            "name", "db_list_tables",
                            "description", "Lista todas as tabelas disponíveis no schema atual do MySQL.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "db_read_table",
                            "description", "Lê dados de uma tabela do schema atual com paginação.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "table", Map.of("type", "string", "description", "Nome da tabela."),
                                            "limit", Map.of("type", "integer", "minimum", 1, "maximum", MAX_LIMIT,
                                                    "description", "Número de linhas retornadas. Padrão: 50."),
                                            "offset", Map.of("type", "integer", "minimum", 0,
                                                    "description", "Deslocamento para paginação. Padrão: 0.")),
                                    "required", List.of("table"),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "db_query",
                            "description", "Executa consulta SQL somente leitura (apenas SELECT/WITH).",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "query", Map.of("type", "string", "description", "SQL SELECT/WITH."),
                                            "limit", Map.of("type", "integer", "minimum", 1, "maximum", MAX_QUERY_LIMIT,
                                                    "description", "Limite de linhas quando a query não tiver LIMIT. Padrão: 100.")),
                                    "required", List.of("query"),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "java_module_logs",
                            "description", "Retorna as últimas linhas de logs do Spring Boot dos módulos Java (backend, ai-worker, lead-portal, facebook-ads, email-service, lead-portal-payment, mds, mois, mois-hotmart, oprm-coletor-receita).",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "module", Map.of("type", "string",
                                                    "enum", List.of("backend", "ai-worker", "lead-portal", "facebook-ads",
                                                            "email-service", "lead-portal-payment", "mds", "mois", "mois-hotmart", "oprm-coletor-receita"),
                                                    "description", "Módulo Java para consultar logs."),
                                            "lines", Map.of("type", "integer", "minimum", 1,
                                                    "maximum", moduleLogService.maxLines(),
                                                    "description", "Quantidade de linhas do final do arquivo de log. Padrão: 200.")),
                                    "required", List.of("module"),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "meta_docs_get",
                            "description", "Busca uma página de documentação da Meta em hosts aprovados e retorna texto simplificado.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "url", Map.of("type", "string",
                                                    "description", "URL HTTPS da documentação da Meta.")),
                                    "required", List.of("url"),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "meta_graph_get",
                            "description", "Executa GET na Graph API da Meta usando token configurado no MCP.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "path", Map.of("type", "string",
                                                    "description", "Path da Graph API, sem versão (ex.: me/adaccounts)."),
                                            "query", Map.of("type", "object",
                                                    "description", "Query string adicional enviada na chamada.")),
                                    "required", List.of("path"),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "meta_graph_debug_token",
                            "description", "Executa debug_token na Graph API para validar um token de usuário/sistema.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "input_token", Map.of("type", "string",
                                                    "description", "Token a ser validado.")),
                                    "required", List.of("input_token"),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "github_actions_list_workflows",
                            "description", "Lista workflows do repositório no GitHub Actions.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "per_page", Map.of("type", "integer", "minimum", 1, "maximum", 100,
                                                    "description", "Quantidade de workflows por página. Padrão: 20.")),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "github_actions_list_runs",
                            "description", "Lista execuções (runs) de workflows do repositório no GitHub Actions.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "branch", Map.of("type", "string", "description", "Filtro por branch."),
                                            "status", Map.of("type", "string", "description", "Filtro por status/conclusion."),
                                            "per_page", Map.of("type", "integer", "minimum", 1, "maximum", 100,
                                                    "description", "Quantidade de runs por página. Padrão: 20.")),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "github_actions_get_run_summary",
                            "description", "Verifica se um workflow run executou com sucesso e retorna erros por job/step quando houver falha.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "run_id", Map.of("type", "integer", "minimum", 1,
                                                    "description", "ID do workflow run no GitHub Actions.")),
                                    "required", List.of("run_id"),
                                    "additionalProperties", false)
                    )))));
            case "tools/call" -> ResponseEntity.ok(callTool(id, request));
            default -> ResponseEntity.ok(error(id, -32601, "Method not found: " + method));
        };
    }

    private boolean isAuthorized(HttpServletRequest request) {
        if (!StringUtils.hasText(properties.apiKey())) {
            return true;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        return StringUtils.hasText(authHeader)
                && authHeader.startsWith(BEARER_PREFIX)
                && properties.apiKey().equals(authHeader.substring(BEARER_PREFIX.length()));
    }

    private Map<String, Object> callTool(Object id, Map<String, Object> request) {
        Object paramsObject = request.get("params");
        if (!(paramsObject instanceof Map<?, ?> params)) {
            return error(id, -32602, "Invalid params");
        }

        Object toolNameValue = params.get("name");
        String toolName = toolNameValue == null ? "" : String.valueOf(toolNameValue);

        try {
            Map<String, Object> arguments = extractArguments(params);
            logger.info("MCP tool call recebido: tool={} requestId={} argumentKeys={}", toolName, id, arguments.keySet());
            return switch (toolName) {
                case "db_health" -> successToolResult(id, databaseDiagnosticsService.checkConnection(),
                        "Database connectivity status");
                case "db_list_tables" -> successToolResult(id, databaseDiagnosticsService.listTables(),
                        "Database tables");
                case "db_read_table" -> callReadTable(id, arguments);
                case "db_query" -> callQueryTool(id, arguments);
                case "java_module_logs" -> callJavaModuleLogsTool(id, arguments);
                case "meta_docs_get" -> callMetaDocsTool(id, arguments);
                case "meta_graph_get" -> callMetaGraphGetTool(id, arguments);
                case "meta_graph_debug_token" -> callMetaGraphDebugTokenTool(id, arguments);
                case "github_actions_list_workflows" -> callGithubActionsListWorkflows(id, arguments);
                case "github_actions_list_runs" -> callGithubActionsListRuns(id, arguments);
                case "github_actions_get_run_summary" -> callGithubActionsGetRunSummary(id, arguments);
                default -> error(id, -32602, "Unknown tool: " + toolName);
            };
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP tool call inválido: tool={} requestId={} motivo={}", toolName, id, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha inesperada em MCP tool call: tool={} requestId={}", toolName, id, ex);
            return error(id, -32603, "Unexpected tool failure: " + ex.getMessage());
        }
    }

    private Map<String, Object> callReadTable(Object id, Map<String, Object> arguments) {
        String table = stringArgument(arguments, "table");

        Integer limitArg = intArgument(arguments, "limit");
        int limit = limitArg == null ? DEFAULT_LIMIT : limitArg;
        if (limit < 1 || limit > MAX_LIMIT) {
            return error(id, -32602, "limit must be between 1 and " + MAX_LIMIT);
        }

        Integer offsetArg = intArgument(arguments, "offset");
        int offset = offsetArg == null ? 0 : offsetArg;
        if (offset < 0) {
            return error(id, -32602, "offset must be greater than or equal to 0");
        }

        try {
            Map<String, Object> result = databaseDiagnosticsService.readTable(table, limit, offset);
            return successToolResult(id, result,
                    "Read " + result.get("returnedRows") + " rows from table " + table);
        } catch (IllegalArgumentException ex) {
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            return error(id, -32603, "Failed to read table: " + ex.getMessage());
        }
    }

    private Map<String, Object> callQueryTool(Object id, Map<String, Object> arguments) {
        String query = stringArgument(arguments, "query");

        Integer limitArg = intArgument(arguments, "limit");
        int limit = limitArg == null ? 100 : limitArg;
        if (limit < 1 || limit > MAX_QUERY_LIMIT) {
            return error(id, -32602, "limit must be between 1 and " + MAX_QUERY_LIMIT);
        }

        try {
            Map<String, Object> result = databaseDiagnosticsService.query(query, limit);
            return successToolResult(id, result,
                    "Query executed with " + result.get("returnedRows") + " rows returned");
        } catch (IllegalArgumentException ex) {
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            return error(id, -32603, "Failed to execute query: " + ex.getMessage());
        }
    }

    private Map<String, Object> callJavaModuleLogsTool(Object id, Map<String, Object> arguments) {
        String module = stringArgument(arguments, "module");
        Integer lines = intArgument(arguments, "lines");

        try {
            Map<String, Object> result = moduleLogService.readModuleLogs(module, lines);
            return successToolResult(id, result, buildJavaModuleLogsText(result));
        } catch (IllegalArgumentException ex) {
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            return error(id, -32603, "Failed to read module logs: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String buildJavaModuleLogsText(Map<String, Object> result) {
        String header = "Read " + result.get("returnedLines") + " log lines from module " + result.get("module");
        Object rawLines = result.get("lines");
        if (!(rawLines instanceof List<?> lines) || lines.isEmpty()) {
            return header;
        }
        String logLines = lines.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\n"));
        return header + "\n" + logLines;
    }

    private Map<String, Object> callMetaDocsTool(Object id, Map<String, Object> arguments) {
        String url = stringArgument(arguments, "url");

        try {
            Map<String, Object> result = metaDiagnosticsService.getDocumentationPage(url);
            return successToolResult(id, result, "Fetched Meta docs page");
        } catch (IllegalArgumentException ex) {
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            return error(id, -32603, "Failed to fetch Meta docs page: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callMetaGraphGetTool(Object id, Map<String, Object> arguments) {
        String path = stringArgument(arguments, "path");
        Object queryObj = arguments.get("query");
        Map<String, Object> query = queryObj instanceof Map<?, ?> queryMap
                ? queryMap.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue))
                : Map.of();

        try {
            Map<String, Object> result = metaDiagnosticsService.graphGet(path, query);
            return successToolResult(id, result, "Meta Graph GET executed");
        } catch (IllegalArgumentException ex) {
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            return error(id, -32603, "Failed to execute Meta Graph GET: " + ex.getMessage());
        }
    }

    private Map<String, Object> callMetaGraphDebugTokenTool(Object id, Map<String, Object> arguments) {
        String inputToken = stringArgument(arguments, "input_token");

        try {
            Map<String, Object> result = metaDiagnosticsService.debugToken(inputToken);
            return successToolResult(id, result, "Meta debug_token executed");
        } catch (IllegalArgumentException ex) {
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            return error(id, -32603, "Failed to execute Meta debug_token: " + ex.getMessage());
        }
    }


    private Map<String, Object> callGithubActionsListWorkflows(Object id, Map<String, Object> arguments) {
        Integer perPage = intArgument(arguments, "per_page");

        try {
            Map<String, Object> result = githubActionsService.listWorkflows(perPage);
            return successToolResult(id, result, "GitHub workflows listed");
        } catch (IllegalArgumentException ex) {
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            return error(id, -32603, "Failed to list GitHub workflows: " + ex.getMessage());
        }
    }

    private Map<String, Object> callGithubActionsListRuns(Object id, Map<String, Object> arguments) {
        String branch = stringArgument(arguments, "branch");
        String status = stringArgument(arguments, "status");
        Integer perPage = intArgument(arguments, "per_page");

        try {
            Map<String, Object> result = githubActionsService.listRuns(branch, status, perPage);
            return successToolResult(id, result, "GitHub workflow runs listed");
        } catch (IllegalArgumentException ex) {
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            return error(id, -32603, "Failed to list GitHub workflow runs: " + ex.getMessage());
        }
    }


    private Map<String, Object> callGithubActionsGetRunSummary(Object id, Map<String, Object> arguments) {
        Integer runIdArg = intArgument(arguments, "run_id");
        Long runId = runIdArg == null ? null : runIdArg.longValue();

        try {
            Map<String, Object> result = githubActionsService.getRunSummary(runId);
            return successToolResult(id, result,
                    Boolean.TRUE.equals(result.get("failed")) ? "Workflow run FAILED" : "Workflow run status fetched");
        } catch (IllegalArgumentException ex) {
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            return error(id, -32603, "Failed to fetch workflow run summary: " + ex.getMessage());
        }
    }

    private Map<String, Object> extractArguments(Map<?, ?> params) {
        Object argumentsObject = params.get("arguments");
        if (argumentsObject == null) {
            return Map.of();
        }

        if (!(argumentsObject instanceof Map<?, ?> rawArguments)) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        return rawArguments.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue));
    }

    private String stringArgument(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        return value == null ? null : String.valueOf(value);
    }

    private Integer intArgument(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapArgument(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException(name + " must be an object");
    }

    private Map<String, Object> successToolResult(Object id, Map<String, Object> data, String text) {
        return success(id, Map.of(
                "content", List.of(Map.of(
                        "type", "text",
                        "text", text)),
                "structuredContent", data
        ));
    }

    private Map<String, Object> success(Object id, Map<String, Object> result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> errorPayload = new LinkedHashMap<>();
        errorPayload.put("code", code);
        errorPayload.put("message", message == null ? "Unexpected error" : message);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", errorPayload);
        return response;
    }
}
