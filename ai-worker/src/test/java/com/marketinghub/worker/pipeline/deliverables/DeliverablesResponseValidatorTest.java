package com.marketinghub.worker.pipeline.deliverables;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliverablesResponseValidatorTest {
    private final DeliverablesResponseValidator validator = new DeliverablesResponseValidator(new ObjectMapper());

    /** Valida que o contrato aceito contém entregáveis de amostra e produto final. */
    @Test
    void validateAndParseAceitaJsonComAmostraEProdutoFinal() {
        String json = """
                {
                  "sampleDeliverables": [
                    {
                      "id": "sample-plan",
                      "name": "Plano rápido de retenção",
                      "format": "PDF",
                      "description": "Mapa prático para aplicar o primeiro check-in do aluno.",
                      "painAddressed": "Falta de percepção de progresso inicial.",
                      "expectedOutcome": "Aluno entende o próximo passo e percebe cuidado."
                    }
                  ],
                  "finalProductDeliverables": [
                    {
                      "id": "full-system",
                      "name": "Sistema completo de onboarding",
                      "format": "Kit digital",
                      "description": "Sequência completa de comunicação, marcos e check-ins.",
                      "painAddressed": "Renovação depende de improviso e indicação.",
                      "expectedOutcome": "Processo repetível para aumentar percepção de valor."
                    }
                  ]
                }
                """;

        DeliverablesOutput output = validator.validateAndParse(json);

        assertThat(output.payload()).containsKeys("sampleDeliverables", "finalProductDeliverables");
    }

    /** Valida que respostas sem produto final são rejeitadas. */
    @Test
    void validateAndParseRejeitaJsonSemProdutoFinal() {
        String json = "{\"sampleDeliverables\":[{\"id\":\"a\"}]}";

        assertThatThrownBy(() -> validator.validateAndParse(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sampleDeliverables and finalProductDeliverables");
    }
}
