package com.marketinghub.worker.pipeline.gerasalespagev1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar enriquecimentos técnicos aplicados pelo processor do GeraSalesPage v1. */
class GeraSalesPageProcessorTest {

    /** Garante que o HTML final registre clique real no checkout para métricas de low-ticket. */
    @Test
    void injectsCheckoutClickTrackingIntoFinalHtml() throws Exception {
        GeraSalesPageProcessor processor = processor();
        Method method = GeraSalesPageProcessor.class.getDeclaredMethod("injectCheckoutClickTracking", String.class);
        method.setAccessible(true);

        String html = (String) method.invoke(processor,
                "<!doctype html><html><body><a href=\"https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=abc\">Comprar</a></body></html>");

        assertThat(html)
                .contains("checkout_click")
                .contains("/api/public/lead-portal/flows/")
                .contains("sendBeacon")
                .contains("</body>");
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
