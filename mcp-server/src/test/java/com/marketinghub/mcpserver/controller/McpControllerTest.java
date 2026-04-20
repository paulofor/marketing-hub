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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class McpControllerTest {

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
    }

    @BeforeEach
    void setupDatabase() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS leads");
        jdbcTemplate.execute("CREATE TABLE leads (id BIGINT PRIMARY KEY, name VARCHAR(100), email VARCHAR(150))");
        jdbcTemplate.update("INSERT INTO leads (id, name, email) VALUES (?,?,?)", 1L, "Ana", "ana@example.com");
        jdbcTemplate.update("INSERT INTO leads (id, name, email) VALUES (?,?,?)", 2L, "Bruno", "bruno@example.com");
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
}

@SpringBootTest(properties = "mcp.api-key=super-secret")
@AutoConfigureMockMvc
class McpControllerApiKeyEnabledTest {

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void setDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:mcpdb2;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
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
