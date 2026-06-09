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
                "IA para salões pequenos", "Salões pequenos", "ROUTINE_REALITY_RESEARCH", new BigDecimal("90.00"),
                "LIGHTLY_RESEARCHED", 84, 100, 0, 87, 82, 72, 35,
                "Rotina com agenda e retorno de clientes.",
                "Dores de falta de tempo e organização.",
                "Perguntas sobre horários e retornos.",
                "Contexto operacional e linguagem do nicho: agenda e encaixes.",
                "Evidência em fontes públicas.",
                "exemplo.com",
                Instant.parse("2026-06-05T00:00:00Z")));

        assertThat(draft.personaSummary()).contains("Cabeleireiros");
        assertThat(draft.personaSummary()).contains("ROUTINE_REALITY_RESEARCH");
        assertThat(draft.languagePatterns()).contains("Dores de falta de tempo");
        assertThat(draft.commercialTriggers()).isNull();
        assertThat(draft.objections()).isNull();
    }

    /** Deve ignorar frases contaminadas por linguagem de solução sem quebrar a materialização aprovada. */
    @Test
    void shouldIgnoreSolutionLanguageWithoutNullPointer() {
        EnrichedNicheMaterializerEngine engine = new EnrichedNicheMaterializerEngine();

        EnrichedNicheProfileDraft draft = engine.buildDraft(new EnrichedNicheMaterializerPending(
                11L, 8L, 1L, null, "9602501", "Cabeleireiros, manicure e pedicure",
                "IA para crescimento de Cabeleireiros, manicure e pedicure", "Cabeleireiros, manicure e pedicure",
                "ROUTINE_REALITY_RESEARCH", new BigDecimal("90.00"), "LIGHTLY_RESEARCHED", 90, 83, 0,
                100, 100, 80, 18,
                "Rotina com sistema de agendamento e automação de retornos.",
                "Dores com aplicativo de agenda e ferramenta de cobrança.",
                "Perguntas sobre encaixes, atrasos e retorno de clientes.",
                "Contexto operacional e linguagem do nicho: controle de horários, encaixes e atendimento.",
                "Evidência em fontes públicas.",
                "exemplo.com",
                Instant.parse("2026-06-08T20:50:04Z")));

        assertThat(draft.personaSummary()).contains("Cabeleireiros");
        assertThat(draft.languagePatterns()).contains("Perguntas sobre encaixes");
        assertThat(draft.languagePatterns()).contains("Contexto operacional");
        assertThat(draft.languagePatterns()).doesNotContain("sistema de agendamento");
        assertThat(draft.languagePatterns()).doesNotContain("aplicativo de agenda");
    }
}
