package com.marketinghub.oprm.nichocnae.v2.service.openaiinteraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Valida o cálculo de custo OpenAI usado nas auditorias do pipeline OPRM NichoCNAE v2. */
class OpenAiInteractionCostCalculatorTest {

    /** Calcula custo de gpt-5.2 com preços oficiais por um milhão de tokens. */
    @Test
    void shouldCalculateGpt52CostFromTokens() {
        OpenAiInteractionAuditRequest request = new OpenAiInteractionAuditRequest(
                "gpt-5.2", "flex", 1_014, 3_430, 4_444, null, "resp_1", "{}", "{}", "completed", null);

        BigDecimal cost = OpenAiInteractionCostCalculator.resolveCostUsd(request);

        assertThat(cost).isEqualByComparingTo("0.0498");
    }

    /** Normaliza variantes datadas do modelo antes de calcular o custo. */
    @Test
    void shouldNormalizeDatedModelVariant() {
        OpenAiInteractionAuditRequest request = new OpenAiInteractionAuditRequest(
                "gpt-5.2-2025-12-11", "flex", 1_000, 1_000, 2_000, null, "resp_1", "{}", "{}", "completed", null);

        BigDecimal cost = OpenAiInteractionCostCalculator.resolveCostUsd(request);

        assertThat(cost).isEqualByComparingTo("0.0158");
    }

    /** Preserva custo legado quando o modelo ainda não tem preço cadastrado no backend. */
    @Test
    void shouldKeepLegacyCostForUnknownModel() {
        OpenAiInteractionAuditRequest request = new OpenAiInteractionAuditRequest(
                "modelo-ausente", "flex", 1_000, 1_000, 2_000, new BigDecimal("0.1234"), "resp_1", "{}", "{}", "completed", null);

        BigDecimal cost = OpenAiInteractionCostCalculator.resolveCostUsd(request);

        assertThat(cost).isEqualByComparingTo("0.1234");
    }
}
