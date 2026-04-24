package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CustomFormHtmlResolverTest {

    private final CustomFormHtmlResolver resolver = new CustomFormHtmlResolver();

    @Test
    void normalizeReturnsHtmlWhenPayloadAlreadyFormatted() {
        String html = "  <html><body><h1>Teste</h1></body></html>  ";
        assertThat(resolver.normalize(html)).isEqualTo("<html><body><h1>Teste</h1></body></html>");
    }

    @Test
    void normalizeRejectsJsonPayload() {
        String payload = "{\"experimentMetadata\":{\"variant_id\":\"variant-10\"},\"landingPageHtml\":{\"htmlDocument\":\"<html><body><p>Payload</p></body></html>\",\"summary\":\"Resumo\"}}";
        assertThatThrownBy(() -> resolver.normalize(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTML puro");
    }

    @Test
    void normalizeRejectsNestedJsonString() {
        String payload = "{\"htmlDocument\":\"{\\\"htmlDocument\\\":\\\"<html><body>Nested</body></html>\\\"}\"}";
        assertThatThrownBy(() -> resolver.normalize(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTML puro");
    }

    @Test
    void normalizeRescuesLegacyWrappedJsonInsideHtmlBody() {
        String payload = """
                <html lang="\\&quot;pt-BR\\&quot;">
                  <head></head>
                  <body>
                    {"landingPageHtml":{"htmlDocument":"<html><body><h1>Exp 14</h1><p class=\\&quot;lead\\&quot;>ok</p></body></html>"}}
                  </body>
                </html>
                """;

        assertThat(resolver.normalize(payload))
                .isEqualTo("<html><body><h1>Exp 14</h1><p class=\"lead\">ok</p></body></html>");
    }
}
