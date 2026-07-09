package com.marketinghub.worker.pipeline.gerasalespagev1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar enriquecimentos técnicos aplicados pelo processor do GeraSalesPage v1. */
class GeraSalesPageProcessorTest {

    /** Garante que o HTML final registre page view, tempo de secao e clique real no checkout para metricas de low-ticket. */
    @Test
    void injectsSalesPageAnalyticsTrackingIntoFinalHtml() throws Exception {
        GeraSalesPageProcessor processor = processor();
        Method method = GeraSalesPageProcessor.class.getDeclaredMethod("injectSalesPageAnalyticsTracking", String.class);
        method.setAccessible(true);

        String html = (String) method.invoke(processor,
                "<!doctype html><html><body><section id=\"hero\"><a href=\"https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=abc\">Comprar</a></section></body></html>");

        assertThat(html)
                .contains("data-mh-sales-page-analytics")
                .contains("page_view")
                .contains("section_view_time")
                .contains("checkout_click")
                .contains("/mh-api/public/lead-portal/flows/")
                .contains("sendBeacon")
                .contains("</body>");
    }

    /** Garante que HTML visual valido seja marcado para passar pela auditoria de transformacao do backend. */
    @Test
    void marksVisualBlocksAsTransformationScenesWhenModelOmitsTechnicalAttribute() throws Exception {
        GeraSalesPageProcessor processor = processor();
        Method method = GeraSalesPageProcessor.class.getDeclaredMethod("ensureTransformationVisualMarkers", String.class);
        method.setAccessible(true);

        String html = (String) method.invoke(processor,
                "<html><body><main>"
                        + "<section><h2>Depois</h2><img src=\"https://cdn.test/depois.jpg\" alt=\"Depois\"></section>"
                        + "<section><h2>Dor</h2><img src=\"https://cdn.test/dor.jpg\" alt=\"Dor\"></section>"
                        + "<section><h2>Preview</h2><img src=\"https://cdn.test/preview.jpg\" alt=\"Preview\"></section>"
                        + "</main></body></html>");

        assertThat(html)
                .contains("data-transform-visual=\"after\"")
                .contains("data-transform-visual=\"pain\"")
                .contains("data-transform-visual=\"preview\"");
    }

    /** Garante que pagina sem imagem receba uma faixa visual minima antes da auditoria final. */
    @Test
    void injectsFallbackTransformationVisualSectionWhenHtmlHasNoConcreteVisualEvidence() throws Exception {
        GeraSalesPageProcessor processor = processor();
        Method method = GeraSalesPageProcessor.class.getDeclaredMethod("ensureTransformationVisualMarkers", String.class);
        method.setAccessible(true);

        String html = (String) method.invoke(processor,
                "<html><body><main><section><h1>Oferta clara</h1><p>Texto aprovado sem imagens.</p></section></main></body></html>");

        assertThat(html)
                .contains("mh-transform-visual-strip")
                .contains("data-transform-visual=\"after\"")
                .contains("data-transform-visual=\"pain\"")
                .contains("data-transform-visual=\"preview\"")
                .contains("<svg")
                .doesNotContain("Depois desejado")
                .doesNotContain("Dor atual")
                .doesNotContain("Preview do produto")
                .doesNotContain("Prova do produto");
    }

    /** Cria processor mínimo para exercitar métodos puros por reflexão. */
    private GeraSalesPageProcessor processor() {
        ObjectMapper objectMapper = new ObjectMapper();
        return new GeraSalesPageProcessor(
                objectMapper,
                mock(OpenAiClientPort.class),
                new GeraSalesPageResponseValidator(objectMapper),
                mock(GeraSalesPageBackendClient.class),
                "default");
    }
}
