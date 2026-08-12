package com.marketinghub.mcpserver.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Valida o contrato HTTP/JSON-RPC público do controller MCP.
 */
@SpringBootTest
@AutoConfigureMockMvc
class McpControllerTest {

    private static final Path TEST_LOG_DIR = Path.of("target/test-logs");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("pdeJdbcTemplate")
    private JdbcTemplate pdeJdbcTemplate;

    @MockBean
    private com.marketinghub.mcpserver.service.ProductDiscoveryWorkerHealthService productDiscoveryWorkerHealthService;

    /**
     * Configura datasource e paths de logs isolados para os testes do controller MCP.
     */
    @DynamicPropertySource
    static void setDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:mcpdb;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("mcp.pde.datasource.url", () -> "jdbc:h2:mem:pde_mcpdb;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("mcp.pde.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("mcp.pde.datasource.username", () -> "sa");
        registry.add("mcp.pde.datasource.password", () -> "");
        registry.add("mcp.logs.backend-path", () -> TEST_LOG_DIR.resolve("backend.log").toString());
        registry.add("mcp.logs.ai-worker-path", () -> TEST_LOG_DIR.resolve("ai-worker.log").toString());
        registry.add("mcp.logs.lead-portal-path", () -> TEST_LOG_DIR.resolve("lead-portal.log").toString());
        registry.add("mcp.logs.facebook-ads-path", () -> TEST_LOG_DIR.resolve("facebook-ads.log").toString());
        registry.add("mcp.logs.email-service-path", () -> TEST_LOG_DIR.resolve("email-service.log").toString());
        registry.add("mcp.logs.lead-portal-payment-path",
                () -> TEST_LOG_DIR.resolve("lead-portal-payment.log").toString());
        registry.add("mcp.logs.mds-path", () -> TEST_LOG_DIR.resolve("mds.log").toString());
        registry.add("mcp.logs.mois-path", () -> TEST_LOG_DIR.resolve("mois.log").toString());
        registry.add("mcp.logs.mois-sales-library-worker-path",
                () -> TEST_LOG_DIR.resolve("mois-sales-library-worker.log").toString());
        registry.add("mcp.logs.mois-hotmart-path", () -> TEST_LOG_DIR.resolve("mois-hotmart.log").toString());
        registry.add("mcp.logs.clickbank-coletor-mois-path", () -> TEST_LOG_DIR.resolve("clickbank-coletor-mois.log").toString());
        registry.add("mcp.logs.oprm-coletor-receita-path", () -> TEST_LOG_DIR.resolve("oprm-coletor-receita.log").toString());
        registry.add("mcp.logs.ops-monitor-worker-path", () -> TEST_LOG_DIR.resolve("ops-monitor-worker.log").toString());
        registry.add("mcp.logs.pde-platform-backend-path",
                () -> TEST_LOG_DIR.resolve("pde-platform-backend.log").toString());
        registry.add("mcp.logs.video-management-service-path",
                () -> TEST_LOG_DIR.resolve("video-management-service.log").toString());
        registry.add("mcp.logs.customer-agent-worker-path",
                () -> TEST_LOG_DIR.resolve("customer-agent-worker.log").toString());
        registry.add("mcp.logs.financial-agent-worker-path",
                () -> TEST_LOG_DIR.resolve("financial-agent-worker.log").toString());
        registry.add("mcp.logs.experiment-strategist-worker-path",
                () -> TEST_LOG_DIR.resolve("experiment-strategist-worker.log").toString());
        registry.add("mcp.logs.meta-ad-approver-worker-path",
                () -> TEST_LOG_DIR.resolve("meta-ad-approver-worker.log").toString());
        registry.add("mcp.logs.max-lines", () -> "500");
        registry.add("mcp.chat-logs.enabled", () -> "true");
        registry.add("mcp.chat-logs.allowed-containers", () -> "marketinghub-fashion-chat,product-discovery-worker");
        registry.add("mcp.chat-logs.docker-command", () -> TEST_LOG_DIR.resolve("docker-fake.sh").toString());
        registry.add("mcp.chat-logs.max-lines", () -> "500");
        registry.add("mcp.chat-logs.timeout-seconds", () -> "5");
        registry.add("mcp.docker-ops.enabled", () -> "true");
        registry.add("mcp.docker-ops.allowed-containers",
                () -> "marketinghub-backend,marketinghub-fashion-chat,product-discovery-worker");
        registry.add("mcp.docker-ops.docker-command", () -> TEST_LOG_DIR.resolve("docker-fake.sh").toString());
        registry.add("mcp.docker-ops.max-lines", () -> "500");
        registry.add("mcp.docker-ops.timeout-seconds", () -> "5");
        registry.add("mcp.docker-ops.restart-enabled", () -> "false");
        registry.add("mcp.build-info.enabled", () -> "true");
        registry.add("mcp.build-info.allowed-modules", () -> "backend,pde-platform-backend");
        registry.add("mcp.build-info.module-info-urls.backend",
                () -> "http://127.0.0.1:1/actuator/info");
        registry.add("mcp.build-info.module-info-urls.pde-platform-backend",
                () -> "http://127.0.0.1:1/actuator/info");
        registry.add("mcp.build-info.timeout-seconds", () -> "1");
        registry.add("mcp.vps-host-inventory.enabled", () -> "true");
        registry.add("mcp.vps-host-inventory.allowed-hosts", () -> "191.252.210.83,191.252.120.96");
        registry.add("mcp.vps-host-inventory.ssh-command", () -> TEST_LOG_DIR.resolve("ssh-fake.sh").toString());
        registry.add("mcp.vps-host-inventory.user", () -> "root");
        registry.add("mcp.vps-host-inventory.identity-file", () -> TEST_LOG_DIR.resolve("id_ed25519").toString());
        registry.add("mcp.vps-host-inventory.known-hosts-file", () -> TEST_LOG_DIR.resolve("known_hosts").toString());
        registry.add("mcp.vps-host-inventory.timeout-seconds", () -> "5");
        registry.add("mcp.product-discovery-worker.enabled", () -> "true");
        registry.add("mcp.product-discovery-worker.container", () -> "product-discovery-worker");
        registry.add("mcp.product-discovery-worker.docker-command", () -> TEST_LOG_DIR.resolve("docker-fake.sh").toString());
        registry.add("mcp.product-discovery-worker.health-url", () -> "http://127.0.0.1:8080/healthz");
        registry.add("mcp.product-discovery-worker.timeout-seconds", () -> "5");
        registry.add("mcp.meta.enabled", () -> "false");
        registry.add("mcp.meta.graph-base-url", () -> "https://graph.facebook.com");
        registry.add("mcp.meta.graph-version", () -> "v23.0");
        registry.add("mcp.meta.docs-allowed-hosts", () -> "developers.facebook.com,business.facebook.com");
        registry.add("mcp.github.enabled", () -> "false");
        registry.add("mcp.github.api-base-url", () -> "https://api.github.com");
        registry.add("mcp.github.owner", () -> "marketinghub");
        registry.add("mcp.github.repo", () -> "marketing-hub");
    }

    /**
     * Prepara dados e arquivos de log usados pelas tools testadas.
     */
    @BeforeEach
    void setupDatabase() throws Exception {
        jdbcTemplate.execute("DROP TABLE IF EXISTS studio_cost_ledger_entry");
        jdbcTemplate.execute("DROP TABLE IF EXISTS sales_video_job");
        jdbcTemplate.execute("DROP TABLE IF EXISTS asset");
        jdbcTemplate.execute("DROP TABLE IF EXISTS image_generation_request");
        jdbcTemplate.execute("CREATE TABLE studio_cost_ledger_entry (id BIGINT PRIMARY KEY, source_type VARCHAR(64), source_id VARCHAR(96), provider_cost_usd DECIMAL(14,6), estimated_cost_usd DECIMAL(14,6), commercial_plan_id BIGINT)");
        jdbcTemplate.execute("CREATE TABLE sales_video_job (id BIGINT PRIMARY KEY, job_type VARCHAR(32), provider_name VARCHAR(64), provider_family VARCHAR(32))");
        jdbcTemplate.execute("CREATE TABLE asset (id BIGINT PRIMARY KEY, type VARCHAR(32), provider VARCHAR(64))");
        jdbcTemplate.execute("CREATE TABLE image_generation_request (id BIGINT PRIMARY KEY, job_id VARCHAR(96))");
        jdbcTemplate.update("INSERT INTO sales_video_job VALUES (1, 'SCENE_RENDER', 'KLING', 'VIDEO')");
        jdbcTemplate.update("INSERT INTO sales_video_job VALUES (2, 'SCENE_RENDER', 'KLING', 'VIDEO')");
        jdbcTemplate.update("INSERT INTO studio_cost_ledger_entry VALUES (1, 'SALES_VIDEO_JOB', '1', 2.50, NULL, NULL)");
        jdbcTemplate.update("INSERT INTO asset VALUES (10, 'AUDIO', 'ELEVENLABS')");
        jdbcTemplate.update("INSERT INTO studio_cost_ledger_entry VALUES (2, 'MEDIA_ASSET', '10', NULL, NULL, 7)");
        jdbcTemplate.update("INSERT INTO image_generation_request VALUES (20, 'image-job-20')");
        jdbcTemplate.update("INSERT INTO studio_cost_ledger_entry VALUES (3, 'IMAGE_GENERATION_REQUEST', 'image-job-20', 0.10, NULL, 7)");
        jdbcTemplate.execute("DROP TABLE IF EXISTS leads");
        jdbcTemplate.execute("CREATE TABLE leads (id BIGINT PRIMARY KEY, name VARCHAR(100), email VARCHAR(150))");
        jdbcTemplate.update("INSERT INTO leads (id, name, email) VALUES (?,?,?)", 1L, "Ana", "ana@example.com");
        jdbcTemplate.update("INSERT INTO leads (id, name, email) VALUES (?,?,?)", 2L, "Bruno", "bruno@example.com");
        jdbcTemplate.execute("DROP TABLE IF EXISTS publication_audit");
        jdbcTemplate.execute("""
                CREATE TABLE publication_audit (
                    id BIGINT PRIMARY KEY,
                    payload LONGVARCHAR,
                    access_token LONGVARCHAR
                )
                """);
        jdbcTemplate.update(
                "INSERT INTO publication_audit (id, payload, access_token) VALUES (?,?,?)",
                1L,
                "{\"objective\":\"SALES\",\"access_token\":\"EAA-real-token-123\",\"url\":\"https://graph.facebook.com/v23.0/act_1/adcreatives?access_token=EAA-url-token-456\"}",
                "EAA-column-token-789");

        pdeJdbcTemplate.execute("DROP TABLE IF EXISTS pde_funnel_events");
        pdeJdbcTemplate.execute("""
                CREATE TABLE pde_funnel_events (
                    id BIGINT PRIMARY KEY,
                    session_id VARCHAR(100),
                    event_type VARCHAR(100)
                )
                """);
        pdeJdbcTemplate.update(
                "INSERT INTO pde_funnel_events (id, session_id, event_type) VALUES (?,?,?)",
                1L, "sessao-pde-1", "PAGE_VIEW");

        Files.createDirectories(TEST_LOG_DIR);
        Files.writeString(TEST_LOG_DIR.resolve("backend.log"),
                "line-1\n"
                        + "2026-07-25T21:07:00Z ERROR Erro HTTP 500 não tratado. requestId=req-500-creative status=500 method=POST endpoint=/api/creatives/10/reject uri=/api/creatives/10/reject\n"
                        + "2026-07-25T21:08:00Z ERROR Erro HTTP 500 não tratado. requestId=req-500-other status=500 method=POST endpoint=/api/other uri=/api/other\n"
                        + "2026-07-25T21:09:00Z INFO Meta payload={\"access_token\":\"EAA-log-token-123\",\"campaign\":\"exp-68\"} path=/v23.0/act_1/adcreatives?access_token=EAA-log-url-token-456 Authorization=Bearer bearer-log-token\n"
                        + "line-2\n"
                        + "line-3\n",
                StandardCharsets.UTF_8);
        Files.writeString(TEST_LOG_DIR.resolve("mois-sales-library-worker.log"),
                "mois-sales-library-worker-line-1\nmois-sales-library-worker-line-2\n",
                StandardCharsets.UTF_8);
        Files.writeString(TEST_LOG_DIR.resolve("ops-monitor-worker.log"),
                "ops-monitor-worker-line-1\nops-monitor-worker-line-2\n",
                StandardCharsets.UTF_8);
        Files.writeString(TEST_LOG_DIR.resolve("pde-platform-backend.log"),
                "pde-platform-backend-line-1\npde-platform-backend-line-2\n",
                StandardCharsets.UTF_8);
        Files.writeString(TEST_LOG_DIR.resolve("video-management-service.log"),
                "video-management-service-line-1\nvideo-management-service-line-2\n",
                StandardCharsets.UTF_8);
        Files.writeString(TEST_LOG_DIR.resolve("customer-agent-worker.log"),
                "observationId=42 codec=h264 callback=received\nobservationId=43 codex=timeout\n",
                StandardCharsets.UTF_8);
        Files.writeString(TEST_LOG_DIR.resolve("financial-agent-worker.log"),
                "jobId=91 decision=BLOCKED_BY_MISSING_SOURCE\njobId=92 reconciliation=completed\n",
                StandardCharsets.UTF_8);
        Files.writeString(TEST_LOG_DIR.resolve("meta-ad-approver-worker.log"),
                "experimentId=88 creativeId=278 status=PROCESSING\nexperimentId=88 creativeId=278 codex=timeout\n",
                StandardCharsets.UTF_8);
        Path fakeDocker = TEST_LOG_DIR.resolve("docker-fake.sh");
        Files.writeString(fakeDocker,
                "#!/usr/bin/env sh\n"
                        + "if [ \"$1\" = \"exec\" ]; then\n"
                        + "  echo '{\"service\":\"product-discovery-worker\",\"status\":\"UP\",\"activeSearchProvider\":\"brave\",\"braveSearch\":{\"keyStatus\":\"CONFIGURED\",\"keySource\":\"file\"},\"polling\":{\"lastPollStatus\":\"COMPLETED\",\"lastPollError\":null},\"lastCycleProcessed\":{\"cycleId\":77,\"status\":\"COMPLETED\"}}'\n"
                        + "  exit 0\n"
                        + "fi\n"
                        + "if [ \"$1\" = \"ps\" ]; then\n"
                        + "  echo 'marketinghub-backend|Up 2 minutes (healthy)|ghcr.io/acme/backend:sha'\n"
                        + "  echo 'product-discovery-worker|Exited (1) 1 minute ago|ghcr.io/acme/product-discovery:sha'\n"
                        + "  exit 0\n"
                        + "fi\n"
                        + "if [ \"$1\" = \"restart\" ]; then\n"
                        + "  echo \"$2\"\n"
                        + "  exit 0\n"
                        + "fi\n"
                        + "echo \"2026-07-12T10:00:00Z Fashion chat service listening on port 8094\"\n"
                        + "echo \"2026-07-12T10:00:00Z Started AdsServiceApplication\"\n"
                        + "echo \"2026-07-12T10:00:01Z health ok\"\n",
                StandardCharsets.UTF_8);
        fakeDocker.toFile().setExecutable(true);
        Path fakeSsh = TEST_LOG_DIR.resolve("ssh-fake.sh");
        Files.writeString(fakeSsh,
                "#!/usr/bin/env sh\n"
                        + "case \"$*\" in\n"
                        + "  *\"lead-portal-backend lead-portal-frontend lead-portal-proxy\"*)\n"
                        + "    echo '__MCP_CONTAINER__'\n"
                        + "    echo 'lead-portal-backend'\n"
                        + "    echo '__MCP_STATUS__'\n"
                        + "    echo 'running|false|0|0|healthy|ghcr.io/acme/lead-portal-backend:sha'\n"
                        + "    echo '__MCP_LOGS__'\n"
                        + "    echo '2026-08-07T20:00:00Z LeadPortalApplication started'\n"
                        + "    exit 0\n"
                        + "    ;;\n"
                        + "  *\"docker logs\"*)\n"
                        + "    echo '__MCP_CONTAINER__'\n"
                        + "    echo 'lead-portal-payments-service-proxy-1'\n"
                        + "    echo '__MCP_STATUS__'\n"
                        + "    echo 'restarting|true|9|1|'\n"
                        + "    echo '__MCP_LOGS__'\n"
                        + "    echo '2026-08-03T10:00:00Z nginx certificate missing'\n"
                        + "    exit 0\n"
                        + "    ;;\n"
                        + "esac\n"
                        + "echo '__MCP_HOSTNAME__'\n"
                        + "echo 'ads-vps'\n"
                        + "echo '__MCP_CPU__'\n"
                        + "echo '4'\n"
                        + "echo '__MCP_MEMORY__'\n"
                        + "echo 'Mem: 7900 3200 4700'\n"
                        + "echo '__MCP_DOCKER__'\n"
                        + "echo 'facebook-ads-worker|Up 2 days|ghcr.io/acme/facebook:sha'\n",
                StandardCharsets.UTF_8);
        fakeSsh.toFile().setExecutable(true);
    }

    /** Garante que o diagnóstico evidencia tentativas ausentes e custos desconhecidos. */
    @Test
    void diagnosesStudioLedgerCoverageBySourceTypeAndProvider() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":82,"method":"tools/call","params":{"name":"studio_ledger_coverage","arguments":{}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.status").value("MISSING_LEDGER_ENTRIES"))
                .andExpect(jsonPath("$.result.structuredContent.attempts").value(4))
                .andExpect(jsonPath("$.result.structuredContent.ledgerEntries").value(3))
                .andExpect(jsonPath("$.result.structuredContent.missingEntries").value(1))
                .andExpect(jsonPath("$.result.structuredContent.unknownCostEntries").value(1))
                .andExpect(jsonPath("$.result.structuredContent.unassignedEntries").value(1));
    }

    /**
     * Garante que o método initialize responde sem autenticação.
     */
    @Test
    void shouldInitializeMcpServer() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.serverInfo.name").value("marketing-hub-mcp"));
    }

    /**
     * Garante que o GET de reachability expõe metadados básicos do MCP.
     */
    @Test
    void shouldExposeGetEndpointForReachabilityChecks() throws Exception {
        mockMvc.perform(get("/mcp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoint").value("/mcp"))
                .andExpect(jsonPath("$.protocol").value("json-rpc-2.0"));
    }

    /**
     * Garante que a tool de identidade de build aparece no contrato MCP.
     */
    @Test
    void shouldListRuntimeBuildInfoTool() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":31,"method":"tools/list","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[?(@.name == 'runtime_build_info')].name")
                        .value(org.hamcrest.Matchers.contains("runtime_build_info")));
    }

    /**
     * Garante que a tool db_health retorna status operacional do banco.
     */
    @Test
    void shouldCallDbHealthTool() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"db_health","arguments":{}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.status").value("ok"));
    }

    /**
     * Garante que a tool db_list_tables lista as tabelas do schema de teste.
     */
    @Test
    void shouldListDatabaseTables() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"db_list_tables","arguments":{}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.tableCount").value(6))
                .andExpect(jsonPath("$.result.structuredContent.tables[2]").value("LEADS"));
    }

    /**
     * Garante que as tools de PDE consultam o datasource dedicado do schema efetivo.
     */
    @Test
    void shouldCallPdeDatabaseTools() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":51,"method":"tools/call","params":{"name":"pde_db_health","arguments":{}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.status").value("ok"))
                .andExpect(jsonPath("$.result.structuredContent.datasourceTarget.jdbcUrl")
                        .value("jdbc:h2:mem:pde_mcpdb;MODE=MySQL;DB_CLOSE_DELAY=-1"));

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":52,"method":"tools/call","params":{"name":"pde_db_query","arguments":{"query":"SELECT event_type FROM pde_funnel_events","limit":10}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.rows[0].EVENT_TYPE").value("PAGE_VIEW"));
    }

    /**
     * Garante que a tool db_read_table retorna linhas paginadas da tabela solicitada.
     */
    @Test
    void shouldReadTableRows() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"db_read_table","arguments":{"table":"leads","limit":1,"offset":0}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.table").value("leads"))
                .andExpect(jsonPath("$.result.structuredContent.returnedRows").value(1))
                .andExpect(jsonPath("$.result.structuredContent.totalRows").value(2))
                .andExpect(jsonPath("$.result.structuredContent.rows[0].EMAIL").value("ana@example.com"));
    }

    /**
     * Garante que a tool db_read_table rejeita nomes de tabela inválidos.
     */
    @Test
    void shouldRejectInvalidTableName() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"db_read_table","arguments":{"table":"leads;drop table leads"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602));
    }

    /**
     * Garante que a tool db_query executa apenas leitura e retorna dados.
     */
    @Test
    void shouldExecuteReadOnlyQueryTool() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":8,"method":"tools/call","params":{"name":"db_query","arguments":{"query":"SELECT id, name FROM leads ORDER BY id","limit":1}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.returnedRows").value(1))
                .andExpect(jsonPath("$.result.structuredContent.rows[0].NAME").value("Ana"));
    }

    /**
     * Garante que consultas de auditoria não devolvem tokens persistidos em payloads históricos.
     */
    @Test
    void shouldMaskSensitiveTokensOnDatabaseQueryTool() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":23,"method":"tools/call","params":{"name":"db_query","arguments":{"query":"SELECT payload, access_token FROM publication_audit","limit":1}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.rows[0].ACCESS_TOKEN").value("[REDACTED]"))
                .andExpect(jsonPath("$.result.structuredContent.rows[0].PAYLOAD")
                        .value(org.hamcrest.Matchers.containsString("\"objective\":\"SALES\"")))
                .andExpect(jsonPath("$.result.structuredContent.rows[0].PAYLOAD")
                        .value(org.hamcrest.Matchers.containsString("\"access_token\":\"[REDACTED]\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("EAA-real-token-123"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("EAA-url-token-456"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("EAA-column-token-789"))));
    }

    /**
     * Garante que a tool db_query rejeita comandos que alteram dados.
     */
    @Test
    void shouldRejectNonSelectQuery() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":9,"method":"tools/call","params":{"name":"db_query","arguments":{"query":"UPDATE leads SET name='X'"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message").value("only SELECT queries are allowed"));
    }

    /**
     * Garante que a tool java_module_logs lê logs de módulo Java configurado.
     */
    @Test
    void shouldReadJavaModuleLogs() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":10,"method":"tools/call","params":{"name":"java_module_logs","arguments":{"module":"backend","lines":2}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.module").value("backend"))
                .andExpect(jsonPath("$.result.structuredContent.returnedLines").value(2))
                .andExpect(jsonPath("$.result.content[0].text").value(org.hamcrest.Matchers.containsString("line-2")))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]").value("line-2"))
                .andExpect(jsonPath("$.result.structuredContent.lines[1]").value("line-3"));
    }

    /**
     * Garante que logs Java retornados pelo MCP não expõem tokens de payloads históricos.
     */
    @Test
    void shouldMaskSensitiveTokensOnJavaModuleLogsTool() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":24,"method":"tools/call","params":{"name":"java_module_logs","arguments":{"module":"backend","lines":10,"contains":"Meta payload"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.returnedLines").value(1))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]")
                        .value(org.hamcrest.Matchers.containsString("campaign\":\"exp-68")))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]")
                        .value(org.hamcrest.Matchers.containsString("access_token\":\"[REDACTED]")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("EAA-log-token-123"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("EAA-log-url-token-456"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("bearer-log-token"))));
    }

    /**
     * Garante que o alias mois-sales-library-worker usa o path de log esperado.
     */
    @Test
    void shouldReadMoisSalesLibraryWorkerLogs() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":16,"method":"tools/call","params":{"name":"java_module_logs","arguments":{"module":"mois-sales-library-worker","lines":1}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.module").value("mois-sales-library-worker"))
                .andExpect(jsonPath("$.result.structuredContent.path")
                        .value(TEST_LOG_DIR.resolve("mois-sales-library-worker.log").toString()))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]")
                        .value("mois-sales-library-worker-line-2"));
    }

    /**
     * Garante que o MCP expõe os logs do executor de monitoria operacional.
     */
    @Test
    void shouldReadOpsMonitorWorkerLogs() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":17,"method":"tools/call","params":{"name":"java_module_logs","arguments":{"module":"ops-monitor-worker","lines":1}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.module").value("ops-monitor-worker"))
                .andExpect(jsonPath("$.result.structuredContent.path")
                        .value(TEST_LOG_DIR.resolve("ops-monitor-worker.log").toString()))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]")
                        .value("ops-monitor-worker-line-2"));
    }

    /**
     * Garante que o MCP expõe os logs do backend PDE do Clube MUSA.
     */
    @Test
    void shouldReadPdePlatformBackendLogs() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":20,"method":"tools/call","params":{"name":"java_module_logs","arguments":{"module":"pde-platform-backend","lines":1}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.module").value("pde-platform-backend"))
                .andExpect(jsonPath("$.result.structuredContent.path")
                        .value(TEST_LOG_DIR.resolve("pde-platform-backend.log").toString()))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]")
                        .value("pde-platform-backend-line-2"));
    }

    /**
     * Garante que o MCP expõe os logs do executor operacional de vídeo.
     */
    @Test
    void shouldReadVideoManagementServiceLogs() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":21,"method":"tools/call","params":{"name":"java_module_logs","arguments":{"module":"video-management-service","lines":1}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.module").value("video-management-service"))
                .andExpect(jsonPath("$.result.structuredContent.path")
                        .value(TEST_LOG_DIR.resolve("video-management-service.log").toString()))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]")
                        .value("video-management-service-line-2"));
    }

    /**
     * Garante que o MCP exponha logs correlacionáveis do executor do Agente Cliente.
     */
    @Test
    void shouldReadCustomerAgentWorkerLogs() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":25,"method":"tools/call","params":{"name":"java_module_logs","arguments":{"module":"customer-agent-worker","lines":1,"contains":"codex"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.module").value("customer-agent-worker"))
                .andExpect(jsonPath("$.result.structuredContent.path")
                        .value(TEST_LOG_DIR.resolve("customer-agent-worker.log").toString()))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]")
                        .value("observationId=43 codex=timeout"));
    }

    /**
     * Garante que o MCP exponha logs correlacionáveis do executor do Agente Financeiro.
     */
    @Test
    void shouldReadFinancialAgentWorkerLogs() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":26,"method":"tools/call","params":{"name":"java_module_logs","arguments":{"module":"financial-agent-worker","lines":1,"contains":"reconciliation"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.module").value("financial-agent-worker"))
                .andExpect(jsonPath("$.result.structuredContent.path")
                        .value(TEST_LOG_DIR.resolve("financial-agent-worker.log").toString()))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]")
                        .value("jobId=92 reconciliation=completed"));
    }

    /**
     * Garante que o MCP exponha logs vivos correlacionáveis do Aprovador Meta.
     */
    @Test
    void shouldReadMetaAdApproverWorkerLogs() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":33,"method":"tools/call","params":{"name":"java_module_logs","arguments":{"module":"meta-ad-approver-worker","lines":2,"contains":"creativeId=278"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.module").value("meta-ad-approver-worker"))
                .andExpect(jsonPath("$.result.structuredContent.path")
                        .value(TEST_LOG_DIR.resolve("meta-ad-approver-worker.log").toString()))
                .andExpect(jsonPath("$.result.structuredContent.lines.length()").value(2));
    }

    /**
     * Garante que a tool java_module_logs rejeita módulos fora da lista permitida.
     */
    @Test
    void shouldRejectInvalidJavaModuleName() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":11,"method":"tools/call","params":{"name":"java_module_logs","arguments":{"module":"unknown"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message")
                        .value("module must be one of: backend, ai-worker, lead-portal, facebook-ads, email-service, lead-portal-payment, mds, mois, mois-sales-library-worker, mois-hotmart, clickbank-coletor-mois, oprm-coletor-receita, ops-monitor-worker, pde-platform-backend, video-management-service, customer-agent-worker, financial-agent-worker, experiment-strategist-worker, meta-ad-approver-worker, landing-generator-agent-worker, product-discovery-worker, growth-operator-worker"));
    }

    /**
     * Garante que a tool chat_container_logs lê logs Docker apenas de container permitido.
     */
    @Test
    void shouldReadChatContainerLogs() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":18,"method":"tools/call","params":{"name":"chat_container_logs","arguments":{"container":"marketinghub-fashion-chat","lines":2,"contains":"Fashion"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.container").value("marketinghub-fashion-chat"))
                .andExpect(jsonPath("$.result.structuredContent.returnedLines").value(1))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]")
                        .value("2026-07-12T10:00:00Z Fashion chat service listening on port 8094"));
    }

    /**
     * Garante que o Product Discovery Worker fica disponível para leitura de logs pelo MCP.
     */
    @Test
    void shouldReadProductDiscoveryWorkerContainerLogs() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":20,"method":"tools/call","params":{"name":"chat_container_logs","arguments":{"container":"product-discovery-worker","lines":2}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.container").value("product-discovery-worker"));
    }

    /**
     * Garante que o health do Product Discovery Worker fica disponível pelo MCP.
     */
    @Test
    void shouldReadProductDiscoveryWorkerHealth() throws Exception {
        when(productDiscoveryWorkerHealthService.readHealth()).thenReturn(Map.of(
                "container", "product-discovery-worker",
                "payload", Map.of(
                        "status", "UP",
                        "activeSearchProvider", "brave",
                        "polling", Map.of("lastPollStatus", "COMPLETED"))));

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":25,"method":"tools/call","params":{"name":"product_discovery_worker_health","arguments":{}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.container").value("product-discovery-worker"))
                .andExpect(jsonPath("$.result.structuredContent.payload.status").value("UP"))
                .andExpect(jsonPath("$.result.structuredContent.payload.activeSearchProvider").value("brave"))
                .andExpect(jsonPath("$.result.structuredContent.payload.polling.lastPollStatus").value("COMPLETED"));
    }

    /**
     * Garante que a tool chat_container_logs rejeita containers fora da allowlist.
     */
    @Test
    void shouldRejectChatContainerOutsideAllowList() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":19,"method":"tools/call","params":{"name":"chat_container_logs","arguments":{"container":"marketinghub-backend","lines":2}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message")
                        .value("container must be one of: marketinghub-fashion-chat, product-discovery-worker"));
    }

    /**
     * Garante que a tool docker_ops lista containers operacionais pelo Docker do host MCP.
     */
    @Test
    void shouldListDockerContainersWithDockerOps() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":26,"method":"tools/call","params":{"name":"docker_ops","arguments":{"action":"ps"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.action").value("ps"))
                .andExpect(jsonPath("$.result.structuredContent.returnedContainers").value(2))
                .andExpect(jsonPath("$.result.structuredContent.containers[0].name").value("marketinghub-backend"))
                .andExpect(jsonPath("$.result.structuredContent.containers[0].allowed").value(true));
    }

    /**
     * Garante que a tool docker_ops lê logs do backend principal quando ele está na allowlist.
     */
    @Test
    void shouldReadBackendLogsWithDockerOps() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":27,"method":"tools/call","params":{"name":"docker_ops","arguments":{"action":"logs","container":"marketinghub-backend","lines":3,"contains":"Started"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.action").value("logs"))
                .andExpect(jsonPath("$.result.structuredContent.container").value("marketinghub-backend"))
                .andExpect(jsonPath("$.result.structuredContent.returnedLines").value(1))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]")
                        .value("2026-07-12T10:00:00Z Started AdsServiceApplication"));
    }

    /**
     * Garante que a tool docker_ops não reinicia containers enquanto restart não estiver habilitado.
     */
    @Test
    void shouldRejectDockerRestartWhenDisabled() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":28,"method":"tools/call","params":{"name":"docker_ops","arguments":{"action":"restart","container":"marketinghub-backend"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message")
                        .value("docker restart is disabled (set mcp.docker-ops.restart-enabled=true)"));
    }

    /**
     * Garante que a tool de inventário VPS consulta somente host liberado.
     */
    @Test
    void shouldInspectAllowedVpsHost() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":29,"method":"tools/call","params":{"name":"vps_host_inventory","arguments":{"host":"191.252.210.83"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.host").value("191.252.210.83"))
                .andExpect(jsonPath("$.result.structuredContent.sections.hostname[0]").value("ads-vps"))
                .andExpect(jsonPath("$.result.structuredContent.sections.cpu[0]").value("4"))
                .andExpect(jsonPath("$.result.structuredContent.sections.docker[0]")
                        .value("facebook-ads-worker|Up 2 days|ghcr.io/acme/facebook:sha"));
    }

    /**
     * Garante que a tool de inventário VPS rejeita host fora da allowlist.
     */
    @Test
    void shouldRejectVpsHostOutsideAllowList() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":30,"method":"tools/call","params":{"name":"vps_host_inventory","arguments":{"host":"10.0.0.1"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message")
                        .value("host must be one of: 191.252.210.83, 191.252.120.96"));
    }

    /**
     * Garante que a tool remota lê apenas o proxy Docker explicitamente permitido.
     */
    @Test
    void shouldReadAllowedRemoteDockerLogs() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":31,"method":"tools/call","params":{"name":"vps_docker_logs","arguments":{"host":"191.252.210.83","target":"lead-portal-payments-proxy","lines":100,"contains":"certificate"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.host").value("191.252.210.83"))
                .andExpect(jsonPath("$.result.structuredContent.target")
                        .value("lead-portal-payments-proxy"))
                .andExpect(jsonPath("$.result.structuredContent.returnedLines").value(1))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]")
                        .value("2026-08-03T10:00:00Z nginx certificate missing"));
    }

    /**
     * Garante que estado e logs da stack do Lead Portal ficam disponíveis por alvo fixo.
     */
    @Test
    void shouldReadLeadPortalStackStatusAndLogs() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":32,"method":"tools/call","params":{"name":"vps_docker_logs","arguments":{"host":"191.252.120.96","target":"lead-portal-stack","lines":100}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.host").value("191.252.120.96"))
                .andExpect(jsonPath("$.result.structuredContent.target").value("lead-portal-stack"))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]").value("__MCP_CONTAINER__"))
                .andExpect(jsonPath("$.result.structuredContent.lines[1]").value("lead-portal-backend"));
    }



    /**
     * Garante que a tool java_module_logs aplica filtro textual e paginação.
     */
    @Test
    void shouldFilterAndPaginateJavaModuleLogs() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":15,"method":"tools/call","params":{"name":"java_module_logs","arguments":{"module":"backend","lines":1,"contains":"line","offset":1}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.returnedLines").value(1))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]").value("line-2"));
    }

    /**
     * Garante que a tool java_module_logs filtra erro HTTP por status, endpoint e requestId.
     */
    @Test
    void shouldFilterJavaModuleLogsByHttpErrorContext() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":22,"method":"tools/call","params":{"name":"java_module_logs","arguments":{"module":"backend","lines":10,"httpStatus":500,"endpoint":"/api/creatives/10/reject","requestId":"req-500-creative"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.returnedLines").value(1))
                .andExpect(jsonPath("$.result.structuredContent.httpStatusFilter").value(500))
                .andExpect(jsonPath("$.result.structuredContent.endpointFilter").value("/api/creatives/10/reject"))
                .andExpect(jsonPath("$.result.structuredContent.requestIdFilter").value("req-500-creative"))
                .andExpect(jsonPath("$.result.structuredContent.lines[0]")
                        .value(org.hamcrest.Matchers.containsString("requestId=req-500-creative")));
    }

    /**
     * Garante que erros JSON-RPC preservam id nulo quando a requisição não informou id.
     */
    @Test
    void shouldHandleErrorResponseWhenRequestIdIsNull() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","method":"unknown_method","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.error.code").value(-32601));
    }

    /**
     * Garante que tools Meta e GitHub continuam anunciadas em tools/list.
     */
    @Test
    void shouldListMetaTools() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"jsonrpc":"2.0","id":12,"method":"tools/list","params":{}}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[*].name").value(org.hamcrest.Matchers.hasItems(
                        "meta_graph_debug_token",
                        "github_actions_list_workflows",
                        "github_actions_list_runs",
                        "github_actions_get_run_summary",
                        "github_actions_get_run_logs")));
    }

    /**
     * Garante que o Product Discovery Worker aparece na enum pública dos logs Docker.
     */
    @Test
    void shouldListProductDiscoveryWorkerInContainerLogTool() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"jsonrpc":"2.0","id":21,"method":"tools/list","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("product-discovery-worker")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("docker_ops")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("vps_host_inventory")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("marketinghub-backend")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("product_discovery_worker_health")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("growth_operator_worker_health")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("growth-operator-worker")));
    }



    /**
     * Garante que headers Authorization enviados por clientes legados são ignorados.
     */
    @Test
    void shouldIgnoreAuthorizationHeaderWhenClientStillSendsBearerToken() throws Exception {
        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer legacy-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":17,"method":"initialize","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.serverInfo.name").value("marketing-hub-mcp"));
    }

    /**
     * Garante que tools GitHub desabilitadas retornam erro explícito.
     */
    @Test
    void shouldRejectGithubToolWhenDisabled() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":14,"method":"tools/call","params":{"name":"github_actions_get_run_logs","arguments":{"run_id":123}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message").value("github tools are disabled (set mcp.github.enabled=true)"));
    }

    /**
     * Garante que tools Meta desabilitadas retornam erro explícito.
     */
    @Test
    void shouldRejectMetaToolWhenDisabled() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":13,"method":"tools/call","params":{"name":"meta_graph_get","arguments":{"path":"me"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message").value("meta tools are disabled (set mcp.meta.enabled=true)"));
    }
}
