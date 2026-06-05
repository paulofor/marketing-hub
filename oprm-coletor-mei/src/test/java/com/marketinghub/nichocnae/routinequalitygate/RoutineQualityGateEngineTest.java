package com.marketinghub.nichocnae.routinequalitygate;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar as regras determinísticas da etapa sete de gate de qualidade. */
class RoutineQualityGateEngineTest {
    private final RoutineQualityGateEngine engine = new RoutineQualityGateEngine();

    /** Deve aprovar rotina rica em evidência mesmo sem qualquer sinal de solução sugerida. */
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
                0,
                3,
                2,
                2,
                0,
                0,
                86,
                84,
                72,
                0));

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
                0,
                1,
                1,
                1,
                0,
                0,
                70,
                65,
                24,
                0));

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
                0,
                3,
                2,
                2,
                0,
                0,
                86,
                84,
                72,
                0));

        assertThat(decision.qualityStatus()).isEqualTo("GENERIC");
        assertThat(decision.readyForHypothesis()).isFalse();
    }

    /** Deve reprovar conteúdo dominado por linguagem de IA ou software mesmo com volume de sinais. */
    @Test
    void shouldRejectContentDominatedBySolutionLanguage() {
        RoutineQualityDecision decision = engine.evaluate(pending(
                text("Rotina fala mais de IA software sistema e automação do que de agenda", 8),
                text("Dores citam software IA app e ferramenta em vez de dificuldade concreta", 8),
                text("Perguntas sobre IA software sistema app e automação", 8),
                text("Contexto operacional com linguagem do nicho", 8),
                6,
                14,
                2,
                2,
                0,
                3,
                1,
                2,
                2,
                9,
                80,
                70,
                72,
                70));

        assertThat(decision.qualityStatus()).isEqualTo("NEEDS_MORE_RESEARCH");
        assertThat(decision.readyForHypothesis()).isFalse();
        assertThat(decision.qualityNotes()).contains("dominadoPorSolucao=true");
    }

    /** Deve reprovar texto duplicado ou genérico ainda que existam contadores positivos. */
    @Test
    void shouldRejectGenericOrDuplicatedText() {
        String duplicated = text("agenda cliente rotina", 10);

        RoutineQualityDecision decision = engine.evaluate(pending(
                duplicated,
                duplicated,
                duplicated,
                duplicated,
                6,
                14,
                2,
                3,
                0,
                3,
                2,
                2,
                0,
                0,
                86,
                84,
                72,
                0));

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
            Integer operationalDifficultySignals,
            Integer mechanismSignals,
            Integer routineTasks,
            Integer commercialObjects,
            Integer languageMarkers,
            Integer solutionRiskSignals,
            Integer routineEvidenceScore,
            Integer difficultyEvidenceScore,
            Integer sourceDiversityScore,
            Integer solutionLanguageRiskScore) {
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
                operationalDifficultySignals,
                mechanismSignals,
                routineTasks,
                commercialObjects,
                languageMarkers,
                solutionRiskSignals,
                routineEvidenceScore,
                difficultyEvidenceScore,
                sourceDiversityScore,
                solutionLanguageRiskScore,
                Instant.parse("2026-06-04T00:00:00Z"));
    }

    /** Repete um fragmento para produzir textos longos o suficiente para simular síntese real. */
    private String text(String fragment, int repetitions) {
        return (fragment + ". ").repeat(repetitions).trim();
    }
}
