package com.marketinghub.mcpserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mcpserver.config.McpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class MetaToolsServiceTest {

    private MetaToolsService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        McpProperties properties = new McpProperties(
                "marketing-hub-mcp",
                "1.0.0",
                null,
                new McpProperties.Logs("a", "b", "c", "d", "e", "f", "g", "h", 45, 500),
                new McpProperties.Meta(
                        true,
                        "https://graph.facebook.com",
                        "v22.0",
                        "system-token",
                        "system-token",
                        List.of("developers.facebook.com")
                )
        );
        service = new MetaToolsService(properties, restTemplate, new ObjectMapper());
    }

    @Test
    void shouldFetchMetaDocumentationFromAllowedHost() {
        server.expect(requestTo("https://developers.facebook.com/docs/marketing-apis/"))
                .andExpect(method(GET))
                .andRespond(withSuccess("<html><body><h1>Meta Docs</h1><p>Marketing API</p></body></html>",
                        MediaType.TEXT_HTML));

        Map<String, Object> result = service.getDocumentationPage("https://developers.facebook.com/docs/marketing-apis/");

        assertThat(result.get("host")).isEqualTo("developers.facebook.com");
        assertThat(result.get("excerpt")).isEqualTo("Meta Docs Marketing API");
        server.verify();
    }

    @Test
    void shouldRejectDocumentationHostOutsideAllowlist() {
        assertThatThrownBy(() -> service.getDocumentationPage("https://example.org/meta-docs"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("host not allowed for meta_docs_get: example.org");
    }

    @Test
    void shouldMaskAccessTokenOnGraphResponseMetadata() {
        server.expect(requestTo("https://graph.facebook.com/v22.0/me?fields=id,name&access_token=system-token"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"id\":\"123\"}", MediaType.APPLICATION_JSON));

        Map<String, Object> result = service.graphGet("me", Map.of("fields", "id,name"));

        assertThat(result.get("graphUrl")).isEqualTo("https://graph.facebook.com/v22.0/me?fields=id,name&access_token=***");
        assertThat(((Map<?, ?>) result.get("payload")).get("id")).isEqualTo("123");
        server.verify();
    }
}
