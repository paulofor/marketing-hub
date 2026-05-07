package com.marketinghub.mcpserver.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class McpControllerTest {

    private static final Path TEST_LOG_DIR = Path.of("target/test-logs");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void setDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:mcpdb;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("mcp.logs.backend-path", () -> TEST_LOG_DIR.resolve("backend.log").toString());
        registry.add("mcp.logs.ai-worker-path", () -> TEST_LOG_DIR.resolve("ai-worker.log").toString());
        registry.add("mcp.logs.lead-portal-path", () -> TEST_LOG_DIR.resolve("lead-portal.log").toString());
        registry.add("mcp.logs.facebook-ads-path", () -> TEST_LOG_DIR.resolve("facebook-ads.log").toString());
        registry.add("mcp.logs.email-service-path", () -> TEST_LOG_DIR.resolve("email-service.log").toString());
        registry.add("mcp.logs.lead-portal-payment-path",
                () -> TEST_LOG_DIR.resolve("lead-portal-payment.log").toString());
        registry.add("mcp.logs.mds-path", () -> TEST_LOG_DIR.resolve("mds.log").toString());
        registry.add("mcp.logs.mois-path", () -> TEST_LOG_DIR.resolve("mois.log").toString());
        registry.add("mcp.logs.mois-hotmart-path", () -> TEST_LOG_DIR.resolve("mois-hotmart.log").toString());
        registry.add("mcp.logs.max-lines", () -> "500");
        registry.add("mcp.meta.enabled", () -> "false");
        registry.add("mcp.meta.graph-base-url", () -> "https://graph.facebook.com");
        registry.add("mcp.meta.graph-version", () -> "v23.0");
        registry.add("mcp.meta.docs-allowed-hosts", () -> "developers.facebook.com,business.facebook.com");
        registry.add("mcp.github.enabled", () -> "false");
        registry.add("mcp.github.api-base-url", () -> "https://api.github.com");
        registry.add("mcp.github.owner", () -> "marketinghub");
        registry.add("mcp.github.repo", () -> "marketing-hub");
    }

    @BeforeEach
    void setupDatabase() throws Exception {
        jdbcTemplate.execute("DROP TABLE IF EXISTS leads");
        jdbcTemplate.execute("CREATE TABLE leads (id BIGINT PRIMARY KEY, name VARCHAR(100), email VARCHAR(150))");
        jdbcTemplate.update("INSERT INTO leads (id, name, email) VALUES (?,?,?)", 1L, "Ana", "ana@example.com");
        jdbcTemplate.update("INSERT INTO leads (id, name, email) VALUES (?,?,?)", 2L, "Bruno", "bruno@example.com");

        Files.createDirectories(TEST_LOG_DIR);
        Files.writeString(TEST_LOG_DIR.resolve("backend.log"),
                "line-1\nline-2\nline-3\n",
                StandardCharsets.UTF_8);
    }

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

    @Test
    void shouldExposeGetEndpointForReachabilityChecks() throws Exception {
        mockMvc.perform(get("/mcp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoint").value("/mcp"))
                .andExpect(jsonPath("$.protocol").value("json-rpc-2.0"));
    }

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

    @Test
    void shouldListDatabaseTables() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"db_list_tables","arguments":{}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.structuredContent.tableCount").value(1))
                .andExpect(jsonPath("$.result.structuredContent.tables[0]").value("LEADS"));
    }

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
                        .value("module must be one of: backend, ai-worker, lead-portal, facebook-ads, email-service, lead-portal-payment, mds, mois, mois-hotmart"));
    }

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

    @Test
    void shouldListMetaTools() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":12,"method":"tools/list","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[7].name").value("meta_graph_debug_token"))
                .andExpect(jsonPath("$.result.tools[8].name").value("github_actions_list_workflows"))
                .andExpect(jsonPath("$.result.tools[9].name").value("github_actions_list_runs"))
                .andExpect(jsonPath("$.result.tools[10].name").value("github_actions_get_run_summary"));
    }


    @Test
    void shouldRejectGithubToolWhenDisabled() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":14,"method":"tools/call","params":{"name":"github_actions_get_run_summary","arguments":{"run_id":123}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message").value("github tools are disabled (set mcp.github.enabled=true)"));
    }

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

@SpringBootTest(properties = "mcp.api-key=super-secret")
@AutoConfigureMockMvc
class McpControllerApiKeyEnabledTest {

    private static final Path TEST_LOG_DIR = Path.of("target/test-logs-api-key");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void setDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:mcpdb2;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("mcp.logs.backend-path", () -> TEST_LOG_DIR.resolve("backend.log").toString());
        registry.add("mcp.logs.ai-worker-path", () -> TEST_LOG_DIR.resolve("ai-worker.log").toString());
        registry.add("mcp.logs.lead-portal-path", () -> TEST_LOG_DIR.resolve("lead-portal.log").toString());
        registry.add("mcp.logs.facebook-ads-path", () -> TEST_LOG_DIR.resolve("facebook-ads.log").toString());
        registry.add("mcp.logs.email-service-path", () -> TEST_LOG_DIR.resolve("email-service.log").toString());
        registry.add("mcp.logs.lead-portal-payment-path",
                () -> TEST_LOG_DIR.resolve("lead-portal-payment.log").toString());
        registry.add("mcp.logs.mds-path", () -> TEST_LOG_DIR.resolve("mds.log").toString());
        registry.add("mcp.logs.mois-path", () -> TEST_LOG_DIR.resolve("mois.log").toString());
        registry.add("mcp.logs.mois-hotmart-path", () -> TEST_LOG_DIR.resolve("mois-hotmart.log").toString());
        registry.add("mcp.logs.max-lines", () -> "500");
        registry.add("mcp.meta.enabled", () -> "false");
        registry.add("mcp.meta.graph-base-url", () -> "https://graph.facebook.com");
        registry.add("mcp.meta.graph-version", () -> "v23.0");
        registry.add("mcp.meta.docs-allowed-hosts", () -> "developers.facebook.com,business.facebook.com");
        registry.add("mcp.github.enabled", () -> "false");
        registry.add("mcp.github.api-base-url", () -> "https://api.github.com");
        registry.add("mcp.github.owner", () -> "marketinghub");
        registry.add("mcp.github.repo", () -> "marketing-hub");
    }

    @Test
    void shouldRejectWhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":3,"method":"initialize","params":{}}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAuthorizeWhenBearerTokenMatches() throws Exception {
        mockMvc.perform(post("/mcp")
                        .header("Authorization", "Bearer super-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":4,"method":"initialize","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.serverInfo.name").value("marketing-hub-mcp"));
    }
}
