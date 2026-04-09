package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class CustomFormHtmlResolverTest {

    private final CustomFormHtmlResolver resolver = new CustomFormHtmlResolver(new ObjectMapper());

    @Test
    void normalizeReturnsHtmlWhenPayloadAlreadyFormatted() {
        String html = "  <html><body><h1>Teste</h1></body></html>  ";
        assertThat(resolver.normalize(html)).isEqualTo("<html><body><h1>Teste</h1></body></html>");
    }

    @Test
    void normalizeExtractsHtmlDocumentFromLandingPagePayload() {
        String payload = "{\"experimentMetadata\":{\"variant_id\":\"variant-10\"},\"landingPageHtml\":{\"htmlDocument\":\"<html><body><p>Payload</p></body></html>\",\"summary\":\"Resumo\"}}";
        assertThat(resolver.normalize(payload))
                .contains("<p>Payload</p>")
                .startsWith("<html>");
    }

    @Test
    void normalizeExtractsHtmlDocumentFromNestedString() {
        String payload = """
                {"artifact": {
                    "content": {
                        "htmlDocument": "{\\"htmlDocument\\":\\"<html><body>Nested</body></html>\\"}"
                    }
                }}
                """;
        assertThat(resolver.normalize(payload)).contains("Nested");
    }
}
