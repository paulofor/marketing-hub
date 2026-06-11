package com.marketinghub.worker.pipeline.hypothesispain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a normalização dos dados de entrada da etapa Dor da hipótese. */
class HypothesisPainInputTest {

    /** Garante que mapa ausente seja normalizado para um contexto vazio. */
    @Test
    void shouldNormalizeMissingPromptDataToEmptyMap() {
        HypothesisPainInput input = new HypothesisPainInput(18L, "hypothesis-pain", "job-1", null);

        assertThat(input.promptData()).isEmpty();
    }

    /** Garante que o mapa normalizado não seja alterado após a criação do input. */
    @Test
    void shouldKeepPromptDataImmutableAfterCreation() {
        Map<String, Object> promptData = new LinkedHashMap<>();
        promptData.put("nicheName", "Salão");

        HypothesisPainInput input = new HypothesisPainInput(18L, "hypothesis-pain", "job-1", promptData);
        promptData.put("nicheName", "Alterado");

        assertThat(input.promptData()).containsEntry("nicheName", "Salão");
        assertThatThrownBy(() -> input.promptData().put("novo", "valor"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
