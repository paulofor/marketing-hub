package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Testa a limpeza dos envelopes OpenAI persistidos pelo pipeline de dossiê de produto. */
class DossierOpenAiTextResponseExtractorTest {
    /** Extrai o campo text quando a resposta vem no envelope padrão da Responses API. */
    @Test
    void extractsTextFromResponsesApiOutputEnvelope() {
        String raw = """
                {
                  "id": "resp_123",
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "{\\\"produto\\\":\\\"limpo\\\"}"
                        }
                      ]
                    }
                  ]
                }
                """;

        String extracted = DossierOpenAiTextResponseExtractor.extract(raw);

        assertThat(extracted).isEqualTo("{\"produto\":\"limpo\"}");
    }

    /** Mantém o response original quando não existe campo text extraível. */
    @Test
    void keepsOriginalResponseWhenTextCannotBeExtracted() {
        String raw = "resposta direta";

        String extracted = DossierOpenAiTextResponseExtractor.extract(raw);

        assertThat(extracted).isEqualTo(raw);
    }
}
