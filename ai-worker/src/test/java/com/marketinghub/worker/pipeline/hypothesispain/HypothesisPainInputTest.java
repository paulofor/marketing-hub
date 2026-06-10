package com.marketinghub.worker.pipeline.hypothesispain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a normalização dos dados de entrada da etapa Dor da hipótese. */
class HypothesisPainInputTest {

    /** Garante que valores nulos opcionais vindos do backend sejam preservados no prompt. */
    @Test
    void shouldPreserveNullPromptValuesReturnedByBackend() {
        Map<String, Object> promptData = new LinkedHashMap<>();
        promptData.put("marketNicheId", 18L);
        promptData.put("nicheName", "Cabeleireiros, manicure e pedicure");
        promptData.put("extraTips", null);

        HypothesisPainInput input = new HypothesisPainInput(18L, "hypothesis-pain", "job-1", promptData);

        assertThat(input.promptData()).containsEntry("extraTips", null);
        assertThat(input.promptData()).containsEntry("nicheName", "Cabeleireiros, manicure e pedicure");
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
