package com.marketinghub.nichocnae.routinesynthesizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a síntese comportamental da etapa seis a partir dos sinais do público MEI/autônomo. */
class RoutineSynthesizerEngineTest {
    private final RoutineSynthesizerEngine engine = new RoutineSynthesizerEngine();

    /** Deve separar rotina, clientes, canais, dores, sonhos, medos e linguagem sem promover solução ou oferta. */
    @Test
    void shouldBuildSeparateBehavioralBlocksWithoutSolutionLanguage() {
        RoutineCardDraft draft = engine.synthesize(pending(List.of(
                signal("ROUTINE_TASK", "Atende clientes em horários apertados e compra materiais", "Agenda e compra materiais entre atendimentos", 90),
                signal("CUSTOMER_ACQUISITION_BEHAVIOR", "Recebe clientes por indicação e orçamento", "Profissional relata indicação e orçamento por mensagem", 88),
                signal("CHANNEL_USAGE", "Usa WhatsApp e Instagram para conversar com clientes", "Contato público por WhatsApp e Instagram", 86),
                signal("OPERATIONAL_PAIN", "Sofre com remarcações e retrabalho", "Cancelamento causa horário vazio e retrabalho", 87),
                signal("EMOTIONAL_PAIN", "Sente insegurança para cobrar", "Relato de insegurança para cobrar preço justo", 84),
                signal("DREAM_SIGNAL", "Quer agenda cheia e renda previsível", "Sonho de agenda cheia e mês previsível", 82),
                signal("FEAR_SIGNAL", "Tem medo de perder cliente por preço", "Medo de perder cliente ao cobrar valor correto", 81),
                signal("LANGUAGE_MARKER", "Fala em horário, encaixe e cliente de indicação", "Linguagem real: encaixe, indicação e preço justo", 79))));

        assertThat(draft.routineSummary()).contains("Rotina");
        assertThat(draft.customerBehaviorSummary()).contains("Comportamento de clientes", "indicação");
        assertThat(draft.channelsSummary()).contains("Canais", "WhatsApp");
        assertThat(draft.operationalPainsSummary()).contains("Dores práticas", "retrabalho");
        assertThat(draft.emotionalPainsSummary()).contains("Dores emocionais", "insegurança");
        assertThat(draft.dreamsSummary()).contains("Sonhos", "agenda cheia");
        assertThat(draft.fearsSummary()).contains("Medos", "perder cliente");
        assertThat(draft.languageSummary()).contains("Linguagem", "encaixe");
        assertThat(draft.mechanismOpportunitiesSummary().toLowerCase()).doesNotContain("automação", "software", "oferta", "produto");
    }

    /** Deve manter aquisição e canais específicos visíveis mesmo quando sinais genéricos têm maior confiança. */
    @Test
    void shouldPrioritizeAcquisitionAndChannelSignalsOverGenericRoutineSignals() {
        RoutineCardDraft draft = engine.synthesize(pending(List.of(
                signal("QUESTION_SIGNAL", "Pergunta genérica sobre horário de atendimento 1", "Perguntas amplas sobre agenda", 99),
                signal("QUESTION_SIGNAL", "Pergunta genérica sobre horário de atendimento 2", "Perguntas amplas sobre agenda", 98),
                signal("QUESTION_SIGNAL", "Pergunta genérica sobre horário de atendimento 3", "Perguntas amplas sobre agenda", 97),
                signal("QUESTION_SIGNAL", "Pergunta genérica sobre horário de atendimento 4", "Perguntas amplas sobre agenda", 96),
                signal("QUESTION_SIGNAL", "Pergunta genérica sobre horário de atendimento 5", "Perguntas amplas sobre agenda", 95),
                signal("QUESTION_SIGNAL", "Pergunta genérica sobre horário de atendimento 6", "Perguntas amplas sobre agenda", 94),
                signal("COMMERCIAL_OBJECT", "Objeto comercial genérico sobre orçamento", "Página cita orçamento sem canal", 93),
                signal("COMMERCIAL_OBJECT", "Objeto comercial genérico sobre preço", "Página cita preço sem canal", 92),
                signal("CUSTOMER_ACQUISITION_BEHAVIOR", "Novas clientes chegam por indicação de vizinhas", "Relato liga novas clientes à indicação", 61),
                signal("CHANNEL_USAGE", "Agenda recebe pedidos pelo WhatsApp e pelo Instagram", "Fonte cita WhatsApp e Instagram", 60),
                signal("COMMERCIAL_TASK", "Responde orçamento por mensagem antes do agendamento", "Fonte cita orçamento por mensagem", 59))));

        assertThat(draft.customerBehaviorSummary()).contains("Novas clientes chegam por indicação de vizinhas");
        assertThat(draft.channelsSummary())
                .contains("Agenda recebe pedidos pelo WhatsApp e pelo Instagram")
                .contains("Responde orçamento por mensagem antes do agendamento");
    }

    /** Deve orientar nova pesquisa comercial quando aquisição e canais não têm sinais próprios. */
    @Test
    void shouldRequestSpecificResearchWhenCommercialBlocksAreEmpty() {
        RoutineCardDraft draft = engine.synthesize(pending(List.of(
                signal("ROUTINE_TASK", "Organiza agenda e compra materiais", "Rotina operacional sem aquisição", 90),
                signal("OPERATIONAL_PAIN", "Sofre com remarcações", "Remarcações prejudicam o dia", 88))));

        assertThat(draft.customerBehaviorSummary())
                .contains("Evidência insuficiente para cartão vendável")
                .contains("pesquisar especificamente como o cliente chega");
        assertThat(draft.channelsSummary())
                .contains("Evidência insuficiente para cartão vendável")
                .contains("pesquisar especificamente quais canais trazem cliente");
    }

    /** Cria uma pendência mínima da etapa seis para o sintetizador local. */
    private RoutineSynthesizerPending pending(List<SignalForRoutineSynthesis> signals) {
        return new RoutineSynthesizerPending(
                77L,
                1001L,
                "9602-5/01",
                "Cabeleireiros",
                "Cabeleireiros autônomos",
                BigDecimal.valueOf(84),
                "RUNNING",
                signals.size(),
                Instant.parse("2026-06-10T00:00:00Z"),
                signals);
    }

    /** Cria um sinal rastreável com evidência curta e domínio brasileiro. */
    private SignalForRoutineSynthesis signal(String type, String text, String evidence, Integer confidence) {
        return new SignalForRoutineSynthesis(1L, 2L, 3L, type, text, evidence, "exemplo.com.br", confidence);
    }
}
