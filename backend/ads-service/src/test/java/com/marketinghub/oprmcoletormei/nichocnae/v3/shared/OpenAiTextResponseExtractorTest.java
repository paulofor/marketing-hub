package com.marketinghub.oprmcoletormei.nichocnae.v3.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Testa a limpeza do response bruto da OpenAI para gravação funcional no NichoCNAE v3. */
class OpenAiTextResponseExtractorTest {
    private final OpenAiTextResponseExtractor extractor = new OpenAiTextResponseExtractor(new ObjectMapper());

    /** Extrai o campo text quando o response vem no envelope padrão da Responses API. */
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
                          "text": "{\\"stage\\":\\"persona-candidate-generator\\",\\"status\\":\\"PERSONAS_CANDIDATAS\\"}"
                        }
                      ]
                    }
                  ]
                }
                """;

        String extracted = extractor.extract(raw);

        assertThat(extracted).isEqualTo("{\"stage\":\"persona-candidate-generator\",\"status\":\"PERSONAS_CANDIDATAS\"}");
    }

    /** Extrai o campo text quando o worker envia diretamente uma mensagem da OpenAI. */
    @Test
    void extractsTextFromDirectMessageContentEnvelope() {
        String raw = """
                {
                  "type": "message",
                  "content": [
                    {
                      "type": "output_text",
                      "text": "resposta limpa"
                    }
                  ]
                }
                """;

        String extracted = extractor.extract(raw);

        assertThat(extracted).isEqualTo("resposta limpa");
    }

    /** Mantém o response original quando não existe envelope JSON da OpenAI. */
    @Test
    void keepsOriginalResponseWhenTextCannotBeExtracted() {
        String raw = "resposta direta sem envelope";

        String extracted = extractor.extract(raw);

        assertThat(extracted).isEqualTo(raw);
    }
}
