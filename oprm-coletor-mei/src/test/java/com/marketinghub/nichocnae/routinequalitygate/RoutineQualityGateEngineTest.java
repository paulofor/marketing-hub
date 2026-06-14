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
                text("Rotina concreta: cortar cabelo, lavar cabelo, escovar cabelo, preparar coloração e finalizar cabelo", 8),
                text("Dores concretas de falta, preço cedo demais, horários vazios e retrabalho na coloração", 8),
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

        assertThat(decision.qualityStatus()).isEqualTo("MEI_AUDIENCE_READY");
        assertThat(decision.readyForHypothesis()).isTrue();
        assertThat(decision.specificityScore()).isGreaterThanOrEqualTo(60);
        assertThat(decision.confidenceScore()).isGreaterThanOrEqualTo(50);
        assertThat(decision.qualityNotes()).contains("proximoMovimentoCodigo=MATERIALIZAR_NICHO");
    }

    /** Deve reprovar como precisa de pesquisa quando o card tem pouco volume de evidência. */
    @Test
    void shouldRequestMoreResearchWhenEvidenceIsWeak() {
        RoutineQualityDecision decision = engine.evaluate(pending(
                text("Rotina com cortar cabelo, lavar cabelo e escovar cabelo para clientes", 5),
                text("Dores com faltas e atraso na preparação da coloração", 5),
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

        assertThat(decision.qualityStatus()).isEqualTo("NEEDS_MORE_MEI_RESEARCH");
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

        assertThat(decision.qualityStatus()).isEqualTo("SOLUTION_CONTAMINATED");
        assertThat(decision.readyForHypothesis()).isFalse();
        assertThat(decision.qualityNotes())
                .contains("dominadoPorSolucao=true")
                .contains("proximoMovimentoCodigo=REFAZER_BUSCA_SEM_SOLUCAO");
    }


    /** Deve bloquear texto contaminado por solução mesmo quando os contadores de risco ainda não vieram preenchidos. */
    @Test
    void shouldRejectSolutionLanguageDetectedOnlyInCardText() {
        RoutineQualityDecision decision = engine.evaluate(pending(
                text("Rotina mistura agenda do cliente com IA software sistema e automação", 8),
                text("Dores citam falta atraso e retrabalho, mas também app ferramenta curso e oferta", 8),
                text("Perguntas sobre template landing page e inteligência artificial para atendimento", 8),
                text("Contexto operacional público do nicho", 8),
                6,
                14,
                2,
                2,
                1,
                0,
                3,
                0,
                2,
                0,
                86,
                84,
                72,
                0));

        assertThat(decision.qualityStatus()).isEqualTo("SOLUTION_CONTAMINATED");
        assertThat(decision.readyForHypothesis()).isFalse();
        assertThat(decision.qualityNotes()).contains("riscoTextualSolucao=").contains("dominadoPorSolucao=true");
    }

    /** Deve reprovar ou colocar em revisão rotina repetida que não revela tarefa real do executor. */
    @Test
    void shouldRejectOrReviewRepeatedGenericRoutine() {
        RoutineQualityDecision decision = engine.evaluate(pending(
                text("Gerenciar rotina de atendimento e agenda do nicho", 8),
                text("Dores concretas de falta, atraso, remarcação, retrabalho e cobrança", 8),
                text("Resultados desejados com agenda cheia e previsibilidade", 8),
                text("Contexto operacional público do nicho", 8),
                6,
                14,
                2,
                3,
                1,
                0,
                4,
                0,
                2,
                0,
                86,
                84,
                72,
                0));

        assertThat(decision.readyForHypothesis()).isFalse();
        assertThat(decision.qualityStatus()).isEqualTo("NEEDS_EXECUTOR_ROUTINE_EVIDENCE");
        assertThat(decision.qualityNotes())
                .contains("rotinaGenericaRepetida=true")
                .contains("rotinaRevelaTarefasReaisExecutor=false");
    }

    /** Deve diferenciar nicho vendável com lacuna de rotina executora de reprovação genérica. */
    @Test
    void shouldRequestExecutorRoutineEvidenceWhenCommercialSignalsAreStrongButRoutineTasksAreMissing() {
        RoutineQualityDecision decision = engine.evaluate(pending(
                text("Rotina observada de agenda, atendimento, organização e acompanhamento de clientes recorrentes", 8),
                text("Dores concretas de falta, remarcação, cliente que some, cobrança, renda instável e retrabalho", 8),
                text("Resultados desejados com agenda cheia, retorno quinzenal, previsibilidade e menos buracos", 8),
                text("Contexto operacional público do nicho", 8),
                6,
                14,
                2,
                3,
                1,
                0,
                4,
                0,
                2,
                0,
                86,
                84,
                72,
                0));

        assertThat(decision.qualityStatus()).isEqualTo("NEEDS_EXECUTOR_ROUTINE_EVIDENCE");
        assertThat(decision.readyForHypothesis()).isFalse();
        assertThat(decision.qualityNotes())
                .contains("rotinaRevelaTarefasReaisExecutor=false")
                .contains("proximoMovimentoCodigo=BUSCAR_TAREFAS_REAIS_EXECUTOR");
    }


    /** Deve aprovar rotina de manicure/cabeleireiro quando há tarefas concretas distintas e evidência suficiente. */
    @Test
    void shouldApproveManicureHairdresserRoutineWithConcreteTasksAndEnoughEvidence() {
        RoutineQualityDecision decision = engine.evaluate(pending(
                text("Rotina concreta: cortar cabelo, lavar cabelo, escovar cabelo, lixar unhas, retirar cutícula e esmaltar unhas", 8),
                text("Dores concretas de atraso, falta, retrabalho na esmaltação e cobrança de pacote", 8),
                text("Perguntas do profissional sobre preço, retorno, pacote e cancelamento", 8),
                text("Contexto operacional público do nicho", 8),
                6,
                16,
                3,
                3,
                2,
                0,
                6,
                0,
                3,
                0,
                90,
                88,
                78,
                0));

        assertThat(decision.qualityStatus()).isEqualTo("MEI_AUDIENCE_READY");
        assertThat(decision.readyForHypothesis()).isTrue();
        assertThat(decision.qualityNotes())
                .contains("tarefasConcretasDistintas=")
                .contains("rotinaRevelaTarefasReaisExecutor=true");
    }

    /** Deve reprovar como genérico quando a síntese não informa evidências e fontes auditáveis. */
    @Test
    void shouldRejectWhenAuditableEvidenceIsMissing() {
        RoutineQualityGatePending pending = new RoutineQualityGatePending(
                10L,
                1001L,
                "Cabeleireiros",
                text("Rotina observada com agenda WhatsApp atendimento cliente e horários", 8),
                text("Dificuldades concretas com falta atraso remarcação retrabalho e cobrança", 8),
                text("Perguntas do profissional sobre preço retorno pacote e cancelamento", 8),
                "",
                "Clientes chegam por indicação, WhatsApp e Instagram, pedem orçamento, retornam por agenda e recorrência.",
                "Canais usados: WhatsApp, Instagram, telefone, Google e indicação de clientes do bairro.",
                "",
                "a.com",
                80,
                6,
                14,
                3,
                2,
                2,
                0,
                3,
                0,
                2,
                0,
                86,
                84,
                72,
                0,
                3,
                2,
                0,
                0,
                1,
                1,
                82,
                80,
                76,
                0,
                0,
                0,
                Instant.parse("2026-06-04T00:00:00Z"));

        RoutineQualityDecision decision = engine.evaluate(pending);

        assertThat(decision.qualityStatus()).isEqualTo("GENERIC");
        assertThat(decision.readyForHypothesis()).isFalse();
        assertThat(decision.qualityNotes()).contains("evidenciaAuditavelBrasil=false");
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


    /** Deve pedir mais pesquisa quando a rotina é suficiente, mas aquisição/canais são placeholders sem evidência comercial. */
    @Test
    void shouldRequestMoreResearchWhenAcquisitionAndChannelsAreGeneric() {
        RoutineQualityGatePending weakCommercialEvidence = new RoutineQualityGatePending(
                10L,
                1001L,
                "Cabeleireiros",
                text("Rotina concreta com agenda WhatsApp atendimento cliente e horários", 8),
                text("Dores concretas com falta atraso remarcação retrabalho e cobrança", 8),
                text("Resultados desejados com previsibilidade e agenda cheia", 8),
                text("Contexto operacional público do nicho", 8),
                "Sem evidência suficiente sobre comportamento de clientes.",
                "Sem evidência suficiente sobre canais usados.",
                "Evidências em fontes públicas brasileiras",
                "a.com.br,b.com.br,c.com.br",
                80,
                4,
                12,
                2,
                2,
                2,
                0,
                3,
                0,
                2,
                0,
                86,
                84,
                72,
                0,
                3,
                2,
                0,
                0,
                2,
                1,
                82,
                80,
                76,
                0,
                0,
                0,
                Instant.parse("2026-06-04T00:00:00Z"));

        RoutineQualityDecision decision = engine.evaluate(weakCommercialEvidence);

        assertThat(decision.qualityStatus()).isEqualTo("NEEDS_MORE_MEI_RESEARCH");
        assertThat(decision.readyForHypothesis()).isFalse();
        assertThat(decision.qualityNotes())
                .contains("resumoComportamentoClienteUtil=false")
                .contains("resumoCanaisUtil=false")
                .contains("faltaEvidenciaAquisicaoCanaisRecorrenciaOuComportamentoClientes=true")
                .contains("proximoMovimentoCodigo=VALIDAR_AQUISICAO_CANAIS");
    }

    /** Deve pedir mais pesquisa quando há rotina, mas a dor ainda não mostra potencial claro de compra. */
    @Test
    void shouldRequestMoreResearchWhenPainIsNotSellableEnough() {
        RoutineQualityGatePending weakSellablePain = new RoutineQualityGatePending(
                10L,
                1001L,
                "Cabeleireiros",
                text("Rotina concreta: cortar cabelo, lavar cabelo, escovar cabelo, preparar coloração e finalizar cabelo", 8),
                text("Dores leves de organização geral e dúvidas simples do atendimento", 8),
                text("Resultados desejados descritos de forma ampla e pouco ligada a compra", 8),
                text("Contexto operacional público do nicho", 8),
                "Clientes entram em contato pelo WhatsApp e Instagram para dúvidas gerais do atendimento.",
                "Canais usados: WhatsApp e Instagram para mensagens de clientes.",
                "Evidências em fontes públicas brasileiras",
                "a.com.br,b.com.br,c.com.br,d.com.br",
                80,
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
                0,
                4,
                2,
                0,
                0,
                1,
                1,
                82,
                80,
                76,
                0,
                0,
                0,
                Instant.parse("2026-06-04T00:00:00Z"));

        RoutineQualityDecision decision = engine.evaluate(weakSellablePain);

        assertThat(decision.qualityStatus()).isEqualTo("NEEDS_MORE_MEI_RESEARCH");
        assertThat(decision.readyForHypothesis()).isFalse();
        assertThat(decision.qualityNotes())
                .contains("dorVendavelSuficiente=false")
                .contains("dorVendavelScore=")
                .contains("proximoMovimentoCodigo=VALIDAR_DOR_VENDAVEL");
    }


    /** Deve bloquear como fonte antiga quando não há quantidade mínima de fontes recentes. */
    @Test
    void shouldRejectOutdatedSources() {
        RoutineQualityGatePending stale = new RoutineQualityGatePending(
                10L,
                1001L,
                "Cabeleireiros",
                text("Rotina concreta com agenda WhatsApp atendimento cliente e horários", 8),
                text("Dores concretas com falta atraso remarcação retrabalho e cobrança", 8),
                text("Perguntas do profissional sobre preço retorno pacote e cancelamento", 8),
                text("Contexto operacional público do nicho", 8),
                "Clientes chegam por indicação, WhatsApp e Instagram, pedem orçamento, retornam por agenda e recorrência.",
                "Canais usados: WhatsApp, Instagram, telefone, Google e indicação de clientes do bairro.",
                "Evidências em fontes públicas brasileiras",
                "a.com.br,b.com.br,c.com.br",
                80,
                4,
                12,
                2,
                2,
                2,
                0,
                3,
                0,
                2,
                0,
                86,
                84,
                72,
                0,
                3,
                0,
                3,
                0,
                1,
                1,
                82,
                80,
                30,
                75,
                0,
                0,
                Instant.parse("2026-06-04T00:00:00Z"));

        RoutineQualityDecision decision = engine.evaluate(stale);

        assertThat(decision.qualityStatus()).isEqualTo("OUTDATED_SOURCES");
        assertThat(decision.readyForHypothesis()).isFalse();
        assertThat(decision.qualityNotes()).contains("proximoMovimentoCodigo=BUSCAR_FONTES_BRASILEIRAS_RECENTES");
    }

    /** Deve bloquear como corporativo quando o perfil não representa dono-operador MEI/autônomo. */
    @Test
    void shouldRejectStructuredBusinessDrift() {
        RoutineQualityGatePending corporate = new RoutineQualityGatePending(
                10L,
                1001L,
                "Cabeleireiros",
                text("Rotina concreta com agenda WhatsApp atendimento cliente e horários", 8),
                text("Dores concretas com falta atraso remarcação retrabalho e cobrança", 8),
                text("Perguntas do profissional sobre preço retorno pacote e cancelamento", 8),
                text("Contexto operacional público do nicho", 8),
                "Clientes chegam por indicação, WhatsApp e Instagram, pedem orçamento, retornam por agenda e recorrência.",
                "Canais usados: WhatsApp, Instagram, telefone, Google e indicação de clientes do bairro.",
                "Evidências em fontes públicas brasileiras",
                "a.com.br,b.com.br,c.com.br",
                80,
                4,
                12,
                2,
                2,
                2,
                0,
                3,
                0,
                2,
                0,
                86,
                84,
                72,
                0,
                3,
                2,
                0,
                3,
                1,
                1,
                35,
                80,
                76,
                0,
                80,
                0,
                Instant.parse("2026-06-04T00:00:00Z"));

        RoutineQualityDecision decision = engine.evaluate(corporate);

        assertThat(decision.qualityStatus()).isEqualTo("TOO_CORPORATE");
        assertThat(decision.readyForHypothesis()).isFalse();
        assertThat(decision.qualityNotes()).contains("proximoMovimentoCodigo=TROCAR_PARA_DONO_OPERADOR");
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
                "Clientes chegam por indicação, WhatsApp e Instagram, pedem orçamento, retornam por agenda e recorrência.",
                "Canais usados: WhatsApp, Instagram, telefone, Google e indicação de clientes do bairro.",
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
                Math.max(3, sourceCount == null ? 3 : sourceCount),
                2,
                0,
                0,
                1,
                1,
                82,
                80,
                76,
                0,
                0,
                0,
                Instant.parse("2026-06-04T00:00:00Z"));
    }

    /** Repete um fragmento para produzir textos longos o suficiente para simular síntese real. */
    private String text(String fragment, int repetitions) {
        return (fragment + ". ").repeat(repetitions).trim();
    }
}
