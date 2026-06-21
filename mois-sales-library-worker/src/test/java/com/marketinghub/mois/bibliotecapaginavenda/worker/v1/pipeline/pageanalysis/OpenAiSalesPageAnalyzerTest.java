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
                new OpenAiProperties("test-key", "http://localhost", "gpt-test", 1000, 1000, null));

        String line = analyzer.buildBatchLine(10L, "https://example.com", "Resumo visual extraído do HTML: total_img=12");

        assertTrue(line.contains("identificar por que este produto alcançou sucesso"));
        assertTrue(line.contains("não inclua sugestões"));
        assertTrue(line.contains("nunca para propor mudanças"));
        assertFalse(line.contains("recommended_images"));
    }
}
