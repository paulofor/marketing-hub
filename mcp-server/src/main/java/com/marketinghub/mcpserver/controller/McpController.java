package com.marketinghub.mcpserver.controller;

import com.marketinghub.mcpserver.config.McpProperties;
import com.marketinghub.mcpserver.service.ChatContainerLogService;
import com.marketinghub.mcpserver.service.DatabaseDiagnosticsService;
import com.marketinghub.mcpserver.service.DockerOperationsService;
import com.marketinghub.mcpserver.service.MetaDiagnosticsService;
import com.marketinghub.mcpserver.service.GithubActionsService;
import com.marketinghub.mcpserver.service.ModuleLogService;
import com.marketinghub.mcpserver.service.PdeDatabaseDiagnosticsService;
import com.marketinghub.mcpserver.service.ProductDiscoveryWorkerHealthService;
import com.marketinghub.mcpserver.service.RuntimeBuildInfoService;
import com.marketinghub.mcpserver.service.SensitiveDataSanitizer;
import com.marketinghub.mcpserver.service.VpsHostInventoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Expõe o endpoint JSON-RPC do MCP e roteia chamadas para ferramentas operacionais.
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int MAX_QUERY_LIMIT = 500;
    private static final List<String> JAVA_LOG_MODULES = List.of("backend", "ai-worker", "lead-portal", "facebook-ads",
            "email-service", "lead-portal-payment", "mds", "mois", "mois-sales-library-worker",
            "mois-hotmart", "clickbank-coletor-mois", "oprm-coletor-receita", "ops-monitor-worker",
            "pde-platform-backend", "video-management-service");

    private final McpProperties properties;
    private final DatabaseDiagnosticsService databaseDiagnosticsService;
    private final PdeDatabaseDiagnosticsService pdeDatabaseDiagnosticsService;
    private final ModuleLogService moduleLogService;
    private final ChatContainerLogService chatContainerLogService;
    private final DockerOperationsService dockerOperationsService;
    private final RuntimeBuildInfoService runtimeBuildInfoService;
    private final VpsHostInventoryService vpsHostInventoryService;
    private final ProductDiscoveryWorkerHealthService productDiscoveryWorkerHealthService;
    private final MetaDiagnosticsService metaDiagnosticsService;
    private final GithubActionsService githubActionsService;
    private final SensitiveDataSanitizer sensitiveDataSanitizer;

    /**
     * Inicializa o controller com os serviços responsáveis pelas ferramentas MCP.
     */
    public McpController(McpProperties properties,
                         @Qualifier("databaseDiagnosticsService") DatabaseDiagnosticsService databaseDiagnosticsService,
                         PdeDatabaseDiagnosticsService pdeDatabaseDiagnosticsService,
                         ModuleLogService moduleLogService,
                         ChatContainerLogService chatContainerLogService,
                         DockerOperationsService dockerOperationsService,
                         RuntimeBuildInfoService runtimeBuildInfoService,
                         VpsHostInventoryService vpsHostInventoryService,
                         ProductDiscoveryWorkerHealthService productDiscoveryWorkerHealthService,
                         MetaDiagnosticsService metaDiagnosticsService,
                         GithubActionsService githubActionsService,
                         SensitiveDataSanitizer sensitiveDataSanitizer) {
        this.properties = properties;
        this.databaseDiagnosticsService = databaseDiagnosticsService;
        this.pdeDatabaseDiagnosticsService = pdeDatabaseDiagnosticsService;
        this.moduleLogService = moduleLogService;
        this.chatContainerLogService = chatContainerLogService;
        this.dockerOperationsService = dockerOperationsService;
        this.runtimeBuildInfoService = runtimeBuildInfoService;
        this.vpsHostInventoryService = vpsHostInventoryService;
        this.productDiscoveryWorkerHealthService = productDiscoveryWorkerHealthService;
        this.metaDiagnosticsService = metaDiagnosticsService;
        this.githubActionsService = githubActionsService;
        this.sensitiveDataSanitizer = sensitiveDataSanitizer;
    }

    /**
     * Descreve o endpoint MCP para verificações simples de reachability.
     */
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

    /**
     * Processa requisições JSON-RPC de inicialização, listagem de tools e execução de tools.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleRequest(@RequestBody Map<String, Object> request,
                                                             HttpServletRequest httpServletRequest) {
        Object id = request.get("id");
        String method = String.valueOf(request.getOrDefault("method", ""));
        logger.info("Nova requisição MCP recebida: requestId={} method={} remoteAddr={} userAgent={}",
                id, method, httpServletRequest.getRemoteAddr(), httpServletRequest.getHeader("User-Agent"));
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
                            "name", "pde_db_health",
                            "description", "Valida conectividade com o schema efetivo do PDE em produção.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "pde_db_list_tables",
                            "description", "Lista tabelas do schema efetivo do PDE em produção.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "pde_db_read_table",
                            "description", "Lê dados de uma tabela do schema efetivo do PDE com paginação.",
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
                            "name", "pde_db_query",
                            "description", "Executa consulta SQL somente leitura no schema efetivo do PDE.",
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
                            "description", "Retorna as últimas linhas de logs do Spring Boot dos módulos Java (backend, ai-worker, lead-portal, facebook-ads, email-service, lead-portal-payment, mds, mois, mois-sales-library-worker, mois-hotmart, clickbank-coletor-mois, oprm-coletor-receita, ops-monitor-worker, pde-platform-backend, video-management-service).",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "module", Map.of("type", "string",
                                                    "enum", JAVA_LOG_MODULES,
                                                    "description", "Módulo Java para consultar logs."),
                                            "lines", Map.of("type", "integer", "minimum", 1,
                                                    "maximum", moduleLogService.maxLines(),
                                                    "description", "Quantidade de linhas retornadas por página. Padrão: 200."),
                                            "contains", Map.of("type", "string", "description", "Filtra linhas que contenham este texto literal."),
                                            "httpStatus", Map.of("type", "integer", "minimum", 100, "maximum", 599,
                                                    "description", "Filtra linhas de erro HTTP pelo status registrado no log, ex.: 500."),
                                            "endpoint", Map.of("type", "string",
                                                    "description", "Filtra linhas pelo endpoint/URI registrado no log, ex.: /api/creatives."),
                                            "requestId", Map.of("type", "string",
                                                    "description", "Filtra linhas pelo requestId/correlationId registrado no log."),
                                            "from", Map.of("type", "string", "description", "Data/hora inicial ISO-8601 UTC (ex: 2026-05-21T04:35:00Z)."),
                                            "to", Map.of("type", "string", "description", "Data/hora final ISO-8601 UTC (ex: 2026-05-21T04:40:00Z)."),
                                            "offset", Map.of("type", "integer", "minimum", 0, "description", "Offset para paginação dentro do conjunto filtrado."),
                                            "cursor", Map.of("type", "string", "description", "Cursor retornado em nextCursor para próxima página.")),
                                    "required", List.of("module"),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "chat_container_logs",
                            "description", "Retorna logs Docker dos containers operacionais permitidos no host do MCP.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "container", Map.of("type", "string",
                                                    "enum", chatContainerLogService.allowedContainers(),
                                                    "description", "Container operacional permitido para consulta."),
                                            "lines", Map.of("type", "integer", "minimum", 1,
                                                    "maximum", chatContainerLogService.maxLines(),
                                                    "description", "Quantidade de linhas retornadas. Padrão: 200."),
                                            "contains", Map.of("type", "string",
                                                    "description", "Filtra linhas que contenham este texto literal.")),
                                    "required", List.of("container"),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "docker_ops",
                            "description", "Executa operações Docker restritas no host do MCP: ps, logs e restart para containers permitidos.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "action", Map.of("type", "string",
                                                    "enum", dockerOperationsService.allowedActions(),
                                                    "description", "Ação Docker permitida: ps, logs ou restart."),
                                            "container", Map.of("type", "string",
                                                    "enum", dockerOperationsService.allowedContainers(),
                                                    "description", "Container permitido para logs ou restart."),
                                            "lines", Map.of("type", "integer", "minimum", 1,
                                                    "maximum", dockerOperationsService.maxLines(),
                                                    "description", "Quantidade de linhas retornadas para action=logs. Padrão: 200."),
                                            "contains", Map.of("type", "string",
                                                    "description", "Filtra linhas de logs que contenham este texto literal.")),
                                    "required", List.of("action"),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "runtime_build_info",
                            "description", "Consulta a identidade de build publicada em runtime por módulos permitidos, incluindo version, commit, branch e build time quando o Actuator expõe esses campos.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "module", Map.of("type", "string",
                                                    "enum", runtimeBuildInfoService.allowedModules(),
                                                    "description", "Módulo permitido para consulta de build info.")),
                                    "required", List.of("module"),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "vps_host_inventory",
                            "description", "Consulta CPU, memória, disco, portas e containers Docker de um VPS permitido via SSH restrito.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "host", Map.of("type", "string",
                                                    "enum", vpsHostInventoryService.allowedHosts(),
                                                    "description", "IP/host do VPS permitido para inventário.")),
                                    "required", List.of("host"),
                                    "additionalProperties", false)
                    ),
                    Map.of(
                            "name", "product_discovery_worker_health",
                            "description", "Consulta o health do Product Discovery Worker e retorna provider ativo, chave configurada, último polling, último erro e último ciclo processado.",
                            "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(),
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
                    ),
                    Map.of(
                            "name", "github_actions_get_run_logs",
                            "description", "Baixa e retorna trecho dos logs de uma execução de workflow no GitHub Actions.",
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

    /**
     * Executa a tool solicitada e converte falhas em respostas JSON-RPC.
     */
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
                case "pde_db_health" -> successToolResult(id, pdeDatabaseDiagnosticsService.checkConnection(),
                        "PDE database connectivity status");
                case "pde_db_list_tables" -> successToolResult(id, pdeDatabaseDiagnosticsService.listTables(),
                        "PDE database tables");
                case "pde_db_read_table" -> callPdeReadTable(id, arguments);
                case "pde_db_query" -> callPdeQueryTool(id, arguments);
                case "java_module_logs" -> callJavaModuleLogsTool(id, arguments);
                case "chat_container_logs" -> callChatContainerLogsTool(id, arguments);
                case "docker_ops" -> callDockerOpsTool(id, arguments);
                case "runtime_build_info" -> callRuntimeBuildInfoTool(id, arguments);
                case "vps_host_inventory" -> callVpsHostInventoryTool(id, arguments);
                case "product_discovery_worker_health" -> callProductDiscoveryWorkerHealthTool(id);
                case "meta_docs_get" -> callMetaDocsTool(id, arguments);
                case "meta_graph_get" -> callMetaGraphGetTool(id, arguments);
                case "meta_graph_debug_token" -> callMetaGraphDebugTokenTool(id, arguments);
                case "github_actions_list_workflows" -> callGithubActionsListWorkflows(id, arguments);
                case "github_actions_list_runs" -> callGithubActionsListRuns(id, arguments);
                case "github_actions_get_run_summary" -> callGithubActionsGetRunSummary(id, arguments);
                case "github_actions_get_run_logs" -> callGithubActionsGetRunLogs(id, arguments);
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

    /**
     * Executa a leitura paginada de uma tabela do banco.
     */
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
            logger.warn("MCP db_read_table inválido: requestId={} table={} motivo={}", id, table, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar db_read_table: requestId={} table={} limit={} offset={}", id, table, limit, offset, ex);
            return error(id, -32603, "Failed to read table: " + ex.getMessage());
        }
    }

    /**
     * Executa uma consulta SQL somente leitura com limite controlado.
     */
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
            logger.warn("MCP db_query inválido: requestId={} limit={} motivo={}", id, limit, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar db_query: requestId={} limit={}", id, limit, ex);
            return error(id, -32603, "Failed to execute query: " + ex.getMessage());
        }
    }

    /**
     * Executa a leitura paginada de uma tabela do schema efetivo do PDE.
     */
    private Map<String, Object> callPdeReadTable(Object id, Map<String, Object> arguments) {
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
            Map<String, Object> result = pdeDatabaseDiagnosticsService.readTable(table, limit, offset);
            return successToolResult(id, result,
                    "Read " + result.get("returnedRows") + " PDE rows from table " + table);
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP pde_db_read_table inválido: requestId={} table={} motivo={}", id, table, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar pde_db_read_table: requestId={} table={} limit={} offset={}", id, table, limit, offset, ex);
            return error(id, -32603, "Failed to read PDE table: " + ex.getMessage());
        }
    }

    /**
     * Executa uma consulta SQL somente leitura no schema efetivo do PDE.
     */
    private Map<String, Object> callPdeQueryTool(Object id, Map<String, Object> arguments) {
        String query = stringArgument(arguments, "query");

        Integer limitArg = intArgument(arguments, "limit");
        int limit = limitArg == null ? 100 : limitArg;
        if (limit < 1 || limit > MAX_QUERY_LIMIT) {
            return error(id, -32602, "limit must be between 1 and " + MAX_QUERY_LIMIT);
        }

        try {
            Map<String, Object> result = pdeDatabaseDiagnosticsService.query(query, limit);
            return successToolResult(id, result,
                    "PDE query executed with " + result.get("returnedRows") + " rows returned");
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP pde_db_query inválido: requestId={} limit={} motivo={}", id, limit, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar pde_db_query: requestId={} limit={}", id, limit, ex);
            return error(id, -32603, "Failed to execute PDE query: " + ex.getMessage());
        }
    }

    /**
     * Lê logs do módulo Java solicitado com filtros opcionais.
     */
    private Map<String, Object> callJavaModuleLogsTool(Object id, Map<String, Object> arguments) {
        String module = stringArgument(arguments, "module");
        Integer lines = intArgument(arguments, "lines");
        String contains = stringArgument(arguments, "contains");
        Integer httpStatus = intArgument(arguments, "httpStatus");
        String endpoint = stringArgument(arguments, "endpoint");
        String requestId = stringArgument(arguments, "requestId");
        String from = stringArgument(arguments, "from");
        String to = stringArgument(arguments, "to");
        Integer offset = intArgument(arguments, "offset");
        String cursor = stringArgument(arguments, "cursor");

        try {
            Map<String, Object> result = moduleLogService.readModuleLogs(
                    module, lines, contains, httpStatus, endpoint, requestId, from, to, offset, cursor);
            return successToolResult(id, result, buildJavaModuleLogsText(result));
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP java_module_logs inválido: requestId={} module={} motivo={}", id, module, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar java_module_logs: requestId={} module={} lines={} contains={} httpStatus={} endpoint={} logRequestId={} from={} to={}",
                    id, module, lines, contains, httpStatus, endpoint, requestId, from, to, ex);
            return error(id, -32603, "Failed to read module logs: " + ex.getMessage());
        }
    }

    /**
     * Formata a resposta textual do tool de logs a partir das linhas retornadas.
     */
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

    /**
     * Lê logs Docker de um container de chat permitido.
     */
    private Map<String, Object> callChatContainerLogsTool(Object id, Map<String, Object> arguments) {
        String container = stringArgument(arguments, "container");
        Integer lines = intArgument(arguments, "lines");
        String contains = stringArgument(arguments, "contains");

        try {
            Map<String, Object> result = chatContainerLogService.readLogs(container, lines, contains);
            return successToolResult(id, result, buildChatContainerLogsText(result));
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP chat_container_logs inválido: requestId={} container={} motivo={}", id, container, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar chat_container_logs: requestId={} container={} lines={} contains={}",
                    id, container, lines, contains, ex);
            return error(id, -32603, "Failed to read chat container logs: " + ex.getMessage());
        }
    }

    /**
     * Formata a resposta textual do tool de logs de containers de chat.
     */
    private String buildChatContainerLogsText(Map<String, Object> result) {
        String header = "Read " + result.get("returnedLines") + " Docker log lines from chat container "
                + result.get("container");
        Object rawLines = result.get("lines");
        if (!(rawLines instanceof List<?> lines) || lines.isEmpty()) {
            return header;
        }
        String logLines = lines.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\n"));
        return header + "\n" + logLines;
    }

    /**
     * Executa operações Docker restritas no host do MCP.
     */
    private Map<String, Object> callDockerOpsTool(Object id, Map<String, Object> arguments) {
        String action = stringArgument(arguments, "action");
        String container = stringArgument(arguments, "container");
        Integer lines = intArgument(arguments, "lines");
        String contains = stringArgument(arguments, "contains");

        try {
            Map<String, Object> result = dockerOperationsService.execute(action, container, lines, contains);
            return successToolResult(id, result, buildDockerOpsText(result));
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP docker_ops inválido: requestId={} action={} container={} motivo={}",
                    id, action, container, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar docker_ops: requestId={} action={} container={} lines={} contains={}",
                    id, action, container, lines, contains, ex);
            return error(id, -32603, "Failed to execute docker operation: " + ex.getMessage());
        }
    }

    /**
     * Formata a resposta textual da tool Docker operacional.
     */
    private String buildDockerOpsText(Map<String, Object> result) {
        String action = String.valueOf(result.get("action"));
        if ("ps".equals(action)) {
            return "Docker ps returned " + result.get("returnedContainers") + " containers";
        }
        if ("restart".equals(action)) {
            return "Docker restart executed for container " + result.get("container");
        }

        String header = "Read " + result.get("returnedLines") + " Docker log lines from container "
                + result.get("container");
        Object rawLines = result.get("lines");
        if (!(rawLines instanceof List<?> lines) || lines.isEmpty()) {
            return header;
        }
        String logLines = lines.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\n"));
        return header + "\n" + logLines;
    }

    /**
     * Consulta a identidade de build publicada pelo runtime de um módulo permitido.
     */
    private Map<String, Object> callRuntimeBuildInfoTool(Object id, Map<String, Object> arguments) {
        String module = stringArgument(arguments, "module");

        try {
            Map<String, Object> result = runtimeBuildInfoService.readBuildInfo(module);
            return successToolResult(id, result, buildRuntimeBuildInfoText(result));
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP runtime_build_info inválido: requestId={} module={} motivo={}",
                    id, module, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar runtime_build_info: requestId={} module={}", id, module, ex);
            return error(id, -32603, "Failed to read runtime build info: " + ex.getMessage());
        }
    }

    /**
     * Formata um resumo textual da identidade de build publicada pelo runtime.
     */
    @SuppressWarnings("unchecked")
    private String buildRuntimeBuildInfoText(Map<String, Object> result) {
        Object summaryObject = result.get("summary");
        Map<String, Object> summary = summaryObject instanceof Map<?, ?> summaryMap
                ? (Map<String, Object>) summaryMap
                : Map.of();
        if (summary.isEmpty()) {
            return "Runtime build info fetched for module " + result.get("module")
                    + ", but no build identity fields were published";
        }
        return "Runtime build info for module %s: version=%s commit=%s branch=%s buildTime=%s"
                .formatted(
                        result.get("module"),
                        summary.getOrDefault("version", ""),
                        summary.getOrDefault("commitId", summary.getOrDefault("commitAbbrev", "")),
                        summary.getOrDefault("branch", ""),
                        summary.getOrDefault("buildTime", ""));
    }

    /**
     * Consulta o inventário físico e operacional de um VPS liberado para o MCP.
     */
    private Map<String, Object> callVpsHostInventoryTool(Object id, Map<String, Object> arguments) {
        String host = stringArgument(arguments, "host");

        try {
            Map<String, Object> result = vpsHostInventoryService.inspect(host);
            return successToolResult(id, result, buildVpsHostInventoryText(result));
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP vps_host_inventory inválido: requestId={} host={} motivo={}",
                    id, host, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar vps_host_inventory: requestId={} host={}", id, host, ex);
            return error(id, -32603, "Failed to inspect VPS host: " + ex.getMessage());
        }
    }

    /**
     * Formata a resposta textual do inventário de VPS.
     */
    private String buildVpsHostInventoryText(Map<String, Object> result) {
        String header = "Read VPS host inventory from " + result.get("host");
        Object rawLines = result.get("lines");
        if (!(rawLines instanceof List<?> lines) || lines.isEmpty()) {
            return header;
        }
        String output = lines.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\n"));
        return header + "\n" + output;
    }

    /**
     * Consulta o health do Product Discovery Worker pelo Docker local do host MCP.
     */
    private Map<String, Object> callProductDiscoveryWorkerHealthTool(Object id) {
        try {
            Map<String, Object> result = productDiscoveryWorkerHealthService.readHealth();
            return successToolResult(id, result, buildProductDiscoveryWorkerHealthText(result));
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP product_discovery_worker_health inválido: requestId={} motivo={}", id, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar product_discovery_worker_health: requestId={}", id, ex);
            return error(id, -32603, "Failed to read product discovery worker health: " + ex.getMessage());
        }
    }

    /**
     * Formata um resumo textual do health do Product Discovery Worker.
     */
    @SuppressWarnings("unchecked")
    private String buildProductDiscoveryWorkerHealthText(Map<String, Object> result) {
        Object payloadObject = result.get("payload");
        if (!(payloadObject instanceof Map<?, ?> payload)) {
            return "Product Discovery Worker health fetched";
        }
        Object pollingObject = payload.get("polling");
        Map<String, Object> polling = pollingObject instanceof Map<?, ?> pollingMap
                ? (Map<String, Object>) pollingMap
                : Map.of();
        return "Product Discovery Worker health: status=%s provider=%s lastPollStatus=%s lastPollError=%s"
                .formatted(
                        payload.get("status"),
                        payload.get("activeSearchProvider"),
                        polling.get("lastPollStatus"),
                        polling.get("lastPollError"));
    }

    /**
     * Busca documentação Meta aprovada quando as ferramentas Meta estão ativas.
     */
    private Map<String, Object> callMetaDocsTool(Object id, Map<String, Object> arguments) {
        String url = stringArgument(arguments, "url");

        try {
            Map<String, Object> result = metaDiagnosticsService.getDocumentationPage(url);
            return successToolResult(id, result, "Fetched Meta docs page");
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP meta_docs_get inválido: requestId={} url={} motivo={}", id, url, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar meta_docs_get: requestId={} url={}", id, url, ex);
            return error(id, -32603, "Failed to fetch Meta docs page: " + ex.getMessage());
        }
    }

    /**
     * Executa uma chamada GET na Graph API da Meta.
     */
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
            logger.warn("MCP meta_graph_get inválido: requestId={} path={} motivo={}", id, path, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar meta_graph_get: requestId={} path={} queryKeys={}", id, path, query.keySet(), ex);
            return error(id, -32603, "Failed to execute Meta Graph GET: " + ex.getMessage());
        }
    }

    /**
     * Executa debug_token na Graph API da Meta.
     */
    private Map<String, Object> callMetaGraphDebugTokenTool(Object id, Map<String, Object> arguments) {
        String inputToken = stringArgument(arguments, "input_token");

        try {
            Map<String, Object> result = metaDiagnosticsService.debugToken(inputToken);
            return successToolResult(id, result, "Meta debug_token executed");
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP meta_graph_debug_token inválido: requestId={} motivo={}", id, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar meta_graph_debug_token: requestId={}", id, ex);
            return error(id, -32603, "Failed to execute Meta debug_token: " + ex.getMessage());
        }
    }


    /**
     * Lista workflows do GitHub Actions configurado.
     */
    private Map<String, Object> callGithubActionsListWorkflows(Object id, Map<String, Object> arguments) {
        Integer perPage = intArgument(arguments, "per_page");

        try {
            Map<String, Object> result = githubActionsService.listWorkflows(perPage);
            return successToolResult(id, result, "GitHub workflows listed");
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP github_actions_list_workflows inválido: requestId={} perPage={} motivo={}", id, perPage, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar github_actions_list_workflows: requestId={} perPage={}", id, perPage, ex);
            return error(id, -32603, "Failed to list GitHub workflows: " + ex.getMessage());
        }
    }

    /**
     * Lista execuções do GitHub Actions com filtros opcionais.
     */
    private Map<String, Object> callGithubActionsListRuns(Object id, Map<String, Object> arguments) {
        String branch = stringArgument(arguments, "branch");
        String status = stringArgument(arguments, "status");
        Integer perPage = intArgument(arguments, "per_page");

        try {
            Map<String, Object> result = githubActionsService.listRuns(branch, status, perPage);
            return successToolResult(id, result, "GitHub workflow runs listed");
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP github_actions_list_runs inválido: requestId={} branch={} status={} perPage={} motivo={}", id, branch, status, perPage, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar github_actions_list_runs: requestId={} branch={} status={} perPage={}", id, branch, status, perPage, ex);
            return error(id, -32603, "Failed to list GitHub workflow runs: " + ex.getMessage());
        }
    }


    /**
     * Retorna o resumo operacional de uma execução do GitHub Actions.
     */
    private Map<String, Object> callGithubActionsGetRunSummary(Object id, Map<String, Object> arguments) {
        Integer runIdArg = intArgument(arguments, "run_id");
        Long runId = runIdArg == null ? null : runIdArg.longValue();

        try {
            Map<String, Object> result = githubActionsService.getRunSummary(runId);
            return successToolResult(id, result,
                    Boolean.TRUE.equals(result.get("failed")) ? "Workflow run FAILED" : "Workflow run status fetched");
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP github_actions_get_run_summary inválido: requestId={} runId={} motivo={}", id, runId, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar github_actions_get_run_summary: requestId={} runId={}", id, runId, ex);
            return error(id, -32603, "Failed to fetch workflow run summary: " + ex.getMessage());
        }
    }



    /**
     * Retorna um trecho dos logs de uma execução do GitHub Actions.
     */
    private Map<String, Object> callGithubActionsGetRunLogs(Object id, Map<String, Object> arguments) {
        Integer runIdArg = intArgument(arguments, "run_id");
        Long runId = runIdArg == null ? null : runIdArg.longValue();

        try {
            Map<String, Object> result = githubActionsService.getRunLogs(runId);
            return successToolResult(id, result, "GitHub workflow run logs fetched");
        } catch (IllegalArgumentException ex) {
            logger.warn("MCP github_actions_get_run_logs inválido: requestId={} runId={} motivo={}", id, runId, ex.getMessage());
            return error(id, -32602, ex.getMessage());
        } catch (Exception ex) {
            logger.error("Falha ao executar github_actions_get_run_logs: requestId={} runId={}", id, runId, ex);
            return error(id, -32603, "Failed to fetch workflow run logs: " + ex.getMessage());
        }
    }

    /**
     * Extrai e valida o objeto de argumentos enviado em tools/call.
     */
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

    /**
     * Lê um argumento textual opcional removendo espaços excedentes.
     */
    private String stringArgument(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Lê um argumento inteiro aceitando números ou strings numéricas.
     */
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
            logger.warn("MCP intArgument inválido: name={} value={}", name, value, ex);
            throw new IllegalArgumentException(name + " must be an integer");
        }
    }

    /**
     * Lê um argumento de mapa opcional usado por tools com query params.
     */
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

    /**
     * Monta uma resposta JSON-RPC de sucesso para tools com conteúdo textual e estruturado.
     */
    private Map<String, Object> successToolResult(Object id, Map<String, Object> data, String text) {
        @SuppressWarnings("unchecked")
        Map<String, Object> sanitizedData = (Map<String, Object>) sensitiveDataSanitizer.sanitize(data);
        String sanitizedText = sensitiveDataSanitizer.sanitizeText(text);
        return success(id, Map.of(
                "content", List.of(Map.of(
                        "type", "text",
                        "text", sanitizedText)),
                "structuredContent", sanitizedData
        ));
    }

    /**
     * Monta uma resposta JSON-RPC de sucesso genérica.
     */
    private Map<String, Object> success(Object id, Map<String, Object> result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    /**
     * Monta uma resposta JSON-RPC de erro preservando o id da requisição.
     */
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
