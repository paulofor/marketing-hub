package com.marketinghub.nichocnae.routinequalitygate;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar as regras determinísticas da etapa sete de gate de qualidade. */
class RoutineQualityGateEngineTest {
    private final RoutineQualityGateEngine engine = new RoutineQualityGateEngine();

    /** Deve aprovar card específico com fontes, sinais e confiança suficientes. */
    @Test
    void shouldApproveSpecificCardWithEnoughEvidence() {
        RoutineQualityDecision decision = engine.evaluate(pending(
                text("Rotina concreta com agenda, WhatsApp, clientes, horários, pacotes e retorno", 8),
                text("Dores concretas de falta, preço cedo demais e horários vazios", 8),
                text("Resultados desejados com agenda cheia e previsibilidade", 8),
                text("Mecanismo com lembretes, reativação e pacote mensal", 8),
                6,
                14,
                2,
                3,
                3,
                3,
                2));

        assertThat(decision.qualityStatus()).isEqualTo("LIGHTLY_RESEARCHED");
        assertThat(decision.readyForHypothesis()).isTrue();
        assertThat(decision.specificityScore()).isGreaterThanOrEqualTo(60);
        assertThat(decision.confidenceScore()).isGreaterThanOrEqualTo(50);
    }

    /** Deve reprovar como precisa de pesquisa quando o card tem pouco volume de evidência. */
    @Test
    void shouldRequestMoreResearchWhenEvidenceIsWeak() {
        RoutineQualityDecision decision = engine.evaluate(pending(
                text("Rotina com agenda e clientes", 5),
                text("Dores com faltas", 5),
                text("Resultados desejados", 5),
                text("Mecanismo de WhatsApp", 5),
                2,
                4,
                1,
                1,
                1,
                1,
                1));

        assertThat(decision.qualityStatus()).isEqualTo("NEEDS_MORE_RESEARCH");
        assertThat(decision.readyForHypothesis()).isFalse();
    }

    /** Deve classificar como genérico quando resumos obrigatórios estão vazios ou repetidos. */
    @Test
    void shouldMarkGenericWhenSummariesAreMissing() {
        RoutineQualityDecision decision = engine.evaluate(pending(
                "rotina genérica",
                "",
                "rotina genérica",
                "",
                6,
                14,
                2,
                3,
                3,
                3,
                2));

        assertThat(decision.qualityStatus()).isEqualTo("GENERIC");
        assertThat(decision.readyForHypothesis()).isFalse();
    }

    /** Cria uma pendência mínima da etapa sete para validar as regras do engine. */
    private RoutineQualityGatePending pending(
            String routineSummary,
            String painsSummary,
            String resultsSummary,
            String mechanismSummary,
            Integer sourceCount,
            Integer signalCount,
            Integer questionSignals,
            Integer painSignals,
            Integer mechanismSignals,
            Integer routineTasks,
            Integer commercialObjects) {
        return new RoutineQualityGatePending(
                10L,
                1001L,
                "Cabeleireiros",
                routineSummary,
                painsSummary,
                resultsSummary,
                mechanismSummary,
                "Evidências em fontes públicas",
                "a.com,b.com,c.com,d.com,e.com,f.com",
                80,
                sourceCount,
                signalCount,
                questionSignals,
                painSignals,
                mechanismSignals,
                routineTasks,
                commercialObjects,
                Instant.parse("2026-06-04T00:00:00Z"));
    }

    /** Repete um fragmento para produzir textos longos o suficiente para simular síntese real. */
    private String text(String fragment, int repetitions) {
        return (fragment + ". ").repeat(repetitions).trim();
    }
}
