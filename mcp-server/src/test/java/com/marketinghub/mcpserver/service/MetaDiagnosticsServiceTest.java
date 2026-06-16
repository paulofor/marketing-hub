package com.marketinghub.mcpserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mcpserver.config.McpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
 * Valida o diagnóstico Meta que consulta documentação, backend e Graph API.
 */
class MetaDiagnosticsServiceTest {

    private MetaDiagnosticsService service;
    private MockRestServiceServer server;

    /**
     * Prepara o serviço com RestTemplate mockado para controlar chamadas HTTP externas.
     */
    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = new MetaDiagnosticsService(properties("fallback-token"), new ObjectMapper(), restTemplate);
    }

    /**
     * Garante que meta_graph_get use primeiro o token ativo retornado pelo backend.
     */
    @Test
    void shouldUseBackendWorkerTokenBeforeFallbackToken() {
        server.expect(requestTo("http://backend/api/accounts/facebook/worker-config"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"accessToken\":\"backend-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://graph.facebook.com/v23.0/search?type=adworkposition&q=Manicure&access_token=backend-token"))
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
     * Garante fallback para token configurado quando o backend não retorna token utilizável.
     */
    @Test
    void shouldFallbackToConfiguredTokenWhenBackendTokenIsBlank() {
        server.expect(requestTo("http://backend/api/accounts/facebook/worker-config"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"accessToken\":\"\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://graph.facebook.com/v23.0/me?access_token=fallback-token"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"id\":\"123\"}", MediaType.APPLICATION_JSON));

        Map<String, Object> result = service.graphGet("me", Map.of());

        assertThat(result.get("url")).isEqualTo("https://graph.facebook.com/v23.0/me?access_token=***");
        assertThat(((Map<?, ?>) result.get("response")).get("id")).isEqualTo("123");
        server.verify();
    }

    /**
     * Cria propriedades MCP com origem de backend e token configurável.
     */
    private McpProperties properties(String fallbackToken) {
        return new McpProperties(
                "marketing-hub-mcp",
                "1.0.0",
                new McpProperties.Logs("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", 45, 3, 400, 500, 262144),
                new McpProperties.Meta(
                        true,
                        "https://graph.facebook.com",
                        "v23.0",
                        fallbackToken,
                        fallbackToken,
                        "http://backend",
                        "/api",
                        List.of("developers.facebook.com")
                ),
                new McpProperties.Github(false, "https://api.github.com", "owner", "repo", "")
        );
    }
}
