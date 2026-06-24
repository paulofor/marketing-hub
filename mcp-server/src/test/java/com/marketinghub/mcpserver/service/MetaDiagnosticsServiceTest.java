package com.marketinghub.mcpserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mcpserver.config.McpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Valida o diagnóstico Meta que consulta documentação, banco e Graph API.
 */
class MetaDiagnosticsServiceTest {

    private MetaDiagnosticsService service;
    private MockRestServiceServer server;
    private JdbcTemplate jdbcTemplate;

    /**
     * Prepara o serviço com banco H2 e RestTemplate mockado para controlar chamadas externas.
     */
    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:meta-diagnostics;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        ));
        jdbcTemplate.execute("DROP TABLE IF EXISTS fb_account");
        jdbcTemplate.execute("""
                CREATE TABLE fb_account (
                    id BIGINT PRIMARY KEY,
                    access_token LONGTEXT,
                    worker_enabled TINYINT(1) NOT NULL
                )
                """);
        service = new MetaDiagnosticsService(properties("fallback-token"), new ObjectMapper(), jdbcTemplate,
                restTemplate);
    }

    /**
     * Garante que meta_graph_get use primeiro o token ativo gravado no banco.
     */
    @Test
    void shouldUseDatabaseWorkerTokenBeforeFallbackToken() {
        jdbcTemplate.update("INSERT INTO fb_account (id, access_token, worker_enabled) VALUES (?,?,?)",
                1L, "database-token", 1);
        server.expect(requestTo("https://graph.facebook.com/v23.0/search?type=adworkposition&q=Manicure&access_token=database-token"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"data\":[{\"id\":\"1\",\"name\":\"Manicures\"}]}",
                        MediaType.APPLICATION_JSON));

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("type", "adworkposition");
        query.put("q", "Manicure");

        Map<String, Object> result = service.graphGet("search", query);

        assertThat(result.get("url")).isEqualTo(
                "https://graph.facebook.com/v23.0/search?type=adworkposition&q=Manicure&access_token=***"
        );
        assertThat(((Map<?, ?>) result.get("response")).get("data")).asList().hasSize(1);
        server.verify();
    }

    /**
     * Garante fallback para token configurado quando o banco não retorna token utilizável.
     */
    @Test
    void shouldFallbackToConfiguredTokenWhenDatabaseTokenIsBlank() {
        jdbcTemplate.update("INSERT INTO fb_account (id, access_token, worker_enabled) VALUES (?,?,?)",
                1L, "", 1);
        server.expect(requestTo("https://graph.facebook.com/v23.0/me?access_token=fallback-token"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"id\":\"123\"}", MediaType.APPLICATION_JSON));

        Map<String, Object> result = service.graphGet("me", Map.of());

        assertThat(result.get("url")).isEqualTo("https://graph.facebook.com/v23.0/me?access_token=***");
        assertThat(((Map<?, ?>) result.get("response")).get("id")).isEqualTo("123");
        server.verify();
    }

    /**
     * Cria propriedades MCP com token configurável.
     */
    private McpProperties properties(String fallbackToken) {
        return new McpProperties(
                "marketing-hub-mcp",
                "1.0.0",
                new McpProperties.Logs("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", 45, 3, 400, 500, 262144),
                new McpProperties.Meta(
                        true,
                        "https://graph.facebook.com",
                        "v23.0",
                        fallbackToken,
                        fallbackToken,
                        List.of("developers.facebook.com")
                ),
                new McpProperties.Github(false, "https://api.github.com", "owner", "repo", "")
        );
    }
}
