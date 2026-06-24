package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenAiSalesPageAnalyzerTest {

    /** Garante que o prompt da análise identifica sucesso observado sem pedir sugestões ao modelo. */
    @Test
    void shouldBuildSuccessDiagnosisPromptWithoutSuggestions() {
        OpenAiSalesPageAnalyzer analyzer = new OpenAiSalesPageAnalyzer(
                RestClient.builder(),
                new OpenAiProperties("test-key", "http://localhost", "gpt-test", 900000, null));

        String payload = analyzer.buildResponsesRequestPayload(10L, "https://example.com", "Resumo visual extraído do HTML: total_img=12");

        assertTrue(payload.contains("identificar por que este produto alcançou sucesso"));
        assertTrue(payload.contains("não inclua sugestões"));
        assertTrue(payload.contains("nunca para propor mudanças"));
        assertTrue(payload.contains("geralanding_wireframe_json"));
        assertTrue(payload.contains("geralanding_image_prompt_json"));
        assertTrue(payload.contains("padrões vencedores observados como insumo reutilizável"));
        assertFalse(payload.contains("recommended_images"));
        assertTrue(payload.contains("\"service_tier\":\"flex\""));
    }
}
