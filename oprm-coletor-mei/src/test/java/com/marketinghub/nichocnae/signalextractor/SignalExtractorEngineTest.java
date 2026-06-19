package com.marketinghub.nichocnae.signalextractor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
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
                .contains("ROUTINE_TASK", "CHANNEL_USAGE", "CUSTOMER_ACQUISITION_BEHAVIOR", "OPERATIONAL_PAIN", "CONTEXT_MARKER", "SOLUTION_LANGUAGE_RISK")
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

    /** Deve preservar tarefas concretas de manicure em vez de reduzir a rotina a uma frase genérica. */
    @Test
    void shouldPreserveSpecificManicureRoutineTasksFromPublicEvidence() {
        var signals = engine.extract(pending(
                "Rotina de manicure autônoma no atendimento",
                "Antes do atendimento, esterilizar alicates e materiais e organizar agenda das clientes.",
                "No serviço, a profissional precisa lixar, retirar cutícula e esmaltar unhas sem atrasar o próximo horário."));

        assertThat(signals).extracting(ExtractedSignal::signalType)
                .contains("ROUTINE_TASK");
        assertThat(signals).extracting(ExtractedSignal::signalText)
                .contains(
                        "Esterilizar alicates e materiais antes do atendimento",
                        "Lixar, retirar cutícula e esmaltar unhas")
                .doesNotContain("Executar rotina diária de atendimento, agenda, materiais e entrega do serviço");
    }

    /** Deve preservar tarefas concretas de cabeleireiro e agenda quando aparecem na evidência pública. */
    @Test
    void shouldPreserveSpecificHairdresserRoutineTasksFromPublicEvidence() {
        var signals = engine.extract(pending(
                "Atendimento de cabeleireiro com agenda pelo WhatsApp",
                "A rotina inclui lavar, cortar, escovar e finalizar cabelo entre um cliente e outro.",
                "Também precisa preparar tintura, química ou hidratação, confirmar horários e remarcar clientes pelo WhatsApp."));

        assertThat(signals).extracting(ExtractedSignal::signalType)
                .contains("ROUTINE_TASK", "CHANNEL_USAGE");
        assertThat(signals).extracting(ExtractedSignal::signalText)
                .contains(
                        "Lavar, cortar, escovar e finalizar cabelo",
                        "Preparar tintura, química ou hidratação",
                        "Confirmar horários e remarcar clientes pelo WhatsApp")
                .doesNotContain("Executar rotina diária de atendimento, agenda, materiais e entrega do serviço");
    }

    /** Deve evitar falso positivo de IA quando a sílaba aparece dentro de palavras comuns. */
    @Test
    void shouldNotCreateSolutionRiskFromIaSyllableInsideCommonWords() {
        var signals = engine.extract(pending(
                "Tarefas diárias de manicure",
                "Rotina e atendimento com higiene e confiança.",
                "Profissionais fazem controle de agenda e materiais para os atendimentos diários."));

        assertThat(signals).extracting(ExtractedSignal::signalType)
                .contains("ROUTINE_TASK", "TRUST_REPUTATION_CONCERN", "CONTEXT_MARKER")
                .doesNotContain("SOLUTION_LANGUAGE_RISK", "MECHANISM_OPPORTUNITY");
    }

    /** Deve criar fallback útil quando não há palavra-chave suficiente no trecho público. */
    @Test
    void shouldCreateLanguageMarkerFallbackWhenKeywordsAreAbsent() {
        var signals = engine.extract(pending("Texto público do nicho", "Vocabulário específico", "Conteúdo curto permitido."));

        assertThat(signals).hasSize(1);
        assertThat(signals.getFirst().signalType()).isEqualTo("LANGUAGE_MARKER");
    }

    /** Deve extrair dimensões comportamentais de MEI/autônomo com evidência curta e auditável. */
    @Test
    void shouldExtractBehavioralSignalsForAutonomousAudience() {
        var signals = engine.extract(pending(
                "MEI autônomo usa WhatsApp, Instagram e indicação para conseguir clientes",
                "Medo de cancelamento, renda instável, preço e reputação no atendimento.",
                "Sonho de agenda cheia, sem tempo, insegurança para cobrar e cliente que desmarcou."));

        assertThat(signals).extracting(ExtractedSignal::signalType)
                .contains(
                        "AUTONOMOUS_WORK_MODE",
                        "CHANNEL_USAGE",
                        "CUSTOMER_ACQUISITION_BEHAVIOR",
                        "FEAR_SIGNAL",
                        "DREAM_SIGNAL",
                        "TIME_PRESSURE",
                        "INCOME_INSTABILITY",
                        "TRUST_REPUTATION_CONCERN",
                        "PRICE_INSECURITY",
                        "CLIENT_NO_SHOW_OR_CANCELLATION");
        assertThat(signals).allSatisfy(signal -> assertThat(signal.evidenceExcerpt()).doesNotContain("<html"));
    }



    /** Ciclo 70: deve bloquear prova positiva quando o trecho é de ator adjacente como companhia aérea. */
    @Test
    void shouldRejectAdjacentActorEvidenceForCycle70() {
        var signals = engine.extract(pending(
                "Cancelamento de voo por companhia aérea",
                "Companhia aérea cancelou o voo e passageiros pediram reembolso.",
                "Esse relato não descreve rotina do executor MEI pesquisado."));

        assertThat(signals).extracting(ExtractedSignal::signalType)
                .contains("SEMANTIC_CONTEXT_MISMATCH")
                .doesNotContain("CLIENT_NO_SHOW_OR_CANCELLATION", "OPERATIONAL_PAIN", "ROUTINE_TASK");
    }

    /** Ciclo 72: cada sinal aprovado deve carregar trecho literal de um campo do snapshot, sem síntese recomposta. */
    @Test
    void shouldUseExactEvidenceSpanForCycle72() {
        String title = "Rotina de manicure autônoma";
        String snippet = "Antes do atendimento, esterilizar alicates e materiais.";
        String excerpt = "A profissional precisa lixar, retirar cutícula e esmaltar unhas.";

        var signals = engine.extract(pending(title, snippet, excerpt));

        assertThat(signals).isNotEmpty();
        assertThat(signals).allSatisfy(signal -> assertThat(List.of(title, snippet, excerpt))
                .anySatisfy(part -> assertThat(part).contains(signal.evidenceExcerpt())));
        assertThat(signals).extracting(ExtractedSignal::evidenceExcerpt)
                .doesNotContain(title + " — " + snippet + " — " + excerpt);
    }

    /** Ciclo 75: deve bloquear ocupação homônima/adjacente antes de virar prova do nicho pesquisado. */
    @Test
    void shouldRejectHomonymousOrAdjacentOccupationForCycle75() {
        var signals = engine.extract(pending(
                "Personal shopper plus size",
                "Personal shopper ajuda consumidoras a escolher roupas em lojas.",
                "Revendedora plus size não descreve rotina de manicure, cabeleireiro ou executor pesquisado."));

        assertThat(signals).extracting(ExtractedSignal::signalType)
                .contains("SEMANTIC_CONTEXT_MISMATCH")
                .doesNotContain("ROUTINE_TASK", "CUSTOMER_ACQUISITION_BEHAVIOR", "OPERATIONAL_PAIN");
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
