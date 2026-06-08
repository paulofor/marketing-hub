package com.marketinghub.nichocnae.signalextractor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a extração determinística de sinais da etapa cinco. */
class SignalExtractorEngineTest {
    private final SignalExtractorEngine engine = new SignalExtractorEngine();

    /** Deve extrair sinais comerciais, de dor e contexto operacional sem antecipar mecanismo de solução. */
    @Test
    void shouldExtractRoutinePainAndOperationalContextSignals() {
        var signals = engine.extract(pending(
                "Como organizar agenda com WhatsApp e IA",
                "Use mensagens para confirmar clientes e evitar faltas.",
                "A rotina do salão melhora quando há controle de agenda, lembretes e automação simples."));

        assertThat(signals).extracting(ExtractedSignal::signalType)
                .contains("ROUTINE_TASK", "COMMERCIAL_TASK", "PAIN_POINT", "CONTEXT_MARKER", "SOLUTION_LANGUAGE_RISK")
                .doesNotContain("MECHANISM_OPPORTUNITY");
        assertThat(signals).extracting(ExtractedSignal::signalText)
                .contains("Termo de solução detectado antes da aprovação da rotina")
                .doesNotContain("Criar mecanismo simples de organização, automação ou apoio por IA");
        assertThat(signals).allSatisfy(signal -> assertThat(signal.evidenceExcerpt()).doesNotContain("<html"));
    }

    /** Deve classificar termos de solução isolados como risco, e não como mecanismo positivo, na etapa cinco. */
    @Test
    void shouldCreateSolutionRiskInsteadOfMechanismSignalFromSolutionTerms() {
        var signals = engine.extract(pending(
                "Sistema com IA para salão",
                "Ferramenta de automação para atendimento.",
                "Software promete solução para agenda e vendas."));

        assertThat(signals).extracting(ExtractedSignal::signalType)
                .contains("SOLUTION_LANGUAGE_RISK")
                .doesNotContain("MECHANISM_OPPORTUNITY");
        assertThat(signals).extracting(ExtractedSignal::signalText)
                .contains("Termo de solução detectado antes da aprovação da rotina")
                .noneMatch(text -> text.toLowerCase().contains("apoio por ia"));
    }


    /** Deve evitar falso positivo de IA quando a sílaba aparece dentro de palavras comuns. */
    @Test
    void shouldNotCreateSolutionRiskFromIaSyllableInsideCommonWords() {
        var signals = engine.extract(pending(
                "Tarefas diárias de manicure",
                "Rotina e atendimento com higiene e confiança.",
                "Profissionais fazem controle de agenda e materiais para os atendimentos diários."));

        assertThat(signals).extracting(ExtractedSignal::signalType)
                .contains("ROUTINE_TASK", "CONTEXT_MARKER", "PROOF_SIGNAL")
                .doesNotContain("SOLUTION_LANGUAGE_RISK", "MECHANISM_OPPORTUNITY");
    }

    /** Deve criar fallback útil quando não há palavra-chave suficiente no trecho público. */
    @Test
    void shouldCreateLanguageMarkerFallbackWhenKeywordsAreAbsent() {
        var signals = engine.extract(pending("Texto público do nicho", "Vocabulário específico", "Conteúdo curto permitido."));

        assertThat(signals).hasSize(1);
        assertThat(signals.getFirst().signalType()).isEqualTo("LANGUAGE_MARKER");
    }

    /** Cria uma pendência mínima para validar o extrator local. */
    private SignalExtractorPending pending(String title, String snippet, String excerpt) {
        return new SignalExtractorPending(
                9001L,
                1001L,
                301L,
                "https://exemplo.com/fonte",
                "exemplo.com",
                title,
                "PUBLIC_CONTENT",
                snippet,
                excerpt,
                Instant.parse("2026-06-04T00:00:00Z"),
                "COMPLETED",
                200,
                "SHORT_EXCERPT_ONLY",
                "PUBLIC_SNIPPET",
                "PENDING",
                Instant.parse("2026-06-04T00:00:00Z"));
    }
}
