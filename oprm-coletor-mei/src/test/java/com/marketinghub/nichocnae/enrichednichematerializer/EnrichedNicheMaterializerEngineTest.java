package com.marketinghub.nichocnae.enrichednichematerializer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a montagem determinística do perfil complementar do nicho enriquecido. */
class EnrichedNicheMaterializerEngineTest {
    /** Deve derivar campos complementares sem criar hipótese, oferta ou experimento. */
    @Test
    void shouldBuildComplementaryDraftWithoutHypothesis() {
        EnrichedNicheMaterializerEngine engine = new EnrichedNicheMaterializerEngine();

        EnrichedNicheProfileDraft draft = engine.buildDraft(new EnrichedNicheMaterializerPending(
                10L, 1001L, 77L, null, "9602501", "Cabeleireiros, manicure e pedicure",
                "IA para salões pequenos", new BigDecimal("90.00"), "LIGHTLY_RESEARCHED", 84, 100, 0,
                "Rotina com agenda e retorno de clientes.",
                "Dores de falta de tempo e organização.",
                "Resultado de preencher horários.",
                "Mecanismo de agenda inteligente.",
                "Evidência em fontes públicas.",
                "exemplo.com",
                Instant.parse("2026-06-05T00:00:00Z")));

        assertThat(draft.personaSummary()).contains("Cabeleireiros");
        assertThat(draft.languagePatterns()).contains("Dores de falta de tempo");
        assertThat(draft.commercialTriggers()).isNull();
        assertThat(draft.objections()).isNull();
    }
}
