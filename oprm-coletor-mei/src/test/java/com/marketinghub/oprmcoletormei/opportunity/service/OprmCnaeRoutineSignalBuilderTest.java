package com.marketinghub.oprmcoletormei.opportunity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeEnrichmentRequestDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityScoreResponseDto;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Valida o enriquecimento determinístico de rotinas CNAE para geração de candidatos de nicho vendáveis.
 */
class OprmCnaeRoutineSignalBuilderTest {
    private final OprmCnaeRoutineSignalBuilder builder = new OprmCnaeRoutineSignalBuilder();

    /** Garante que CNAEs de beleza geram sinais concretos de rotina, dor e oferta sem JSON textual. */
    @Test
    void shouldBuildBeautyRoutineSignalsWithoutJsonText() {
        OprmCnaeEnrichmentRequestDto enrichment = builder.buildEnrichment(score("9602501", "Cabeleireiros, manicure e pedicure"), "OPRM-CNAE-ENRICHMENT-20260601-001");

        assertThat(enrichment.routineSignals()).contains("agenda", "pós-atendimento", "Cabeleireiros");
        assertThat(enrichment.painSignals()).contains("faltas na agenda", "previsibilidade");
        assertThat(enrichment.mechanismSignals()).contains("mensagens prontas de WhatsApp", "IA");
        assertThat(enrichment.sourceSummary()).contains("descrição oficial do CNAE", "beleza e bem-estar local");
        assertThat(enrichment.routineSignals()).doesNotContain("{", "}");
        assertThat(enrichment.candidates()).hasSize(1);
        assertThat(enrichment.candidates().getFirst().candidateNicheName()).contains("Kit Agenda Cheia com IA");
        assertThat(enrichment.candidates().getFirst().sourceArtifacts()).contains("oprm-cnae-routine-signal-builder-v2");
    }

    /** Garante que CNAEs de serviços técnicos geram candidato com prova e mecanismo aderentes a orçamento em campo. */
    @Test
    void shouldBuildTechnicalServiceRoutineSignals() {
        OprmCnaeEnrichmentRequestDto enrichment = builder.buildEnrichment(score("4321500", "Instalação e manutenção elétrica"), "OPRM-CNAE-ENRICHMENT-20260601-002");

        assertThat(enrichment.routineSignals()).contains("orçamento", "executar", "cobrar");
        assertThat(enrichment.proofSignals()).contains("taxa de aprovação de orçamento");
        assertThat(enrichment.offerSignals()).contains("Kit Orçamento Aprovado com IA");
        assertThat(enrichment.candidates().getFirst().sourceArtifacts()).contains("source=oprm-cnae-routine-signal-builder-v2");
    }

    /** Monta um score de teste com métricas suficientes para enriquecer rotina e candidato de nicho. */
    private OprmCnaeOpportunityScoreResponseDto score(String cnaeCode, String cnaeDescription) {
        return new OprmCnaeOpportunityScoreResponseDto(
                cnaeCode,
                cnaeDescription,
                BigDecimal.valueOf(87.55),
                BigDecimal.valueOf(91.10),
                BigDecimal.valueOf(82.20),
                BigDecimal.valueOf(85.00),
                BigDecimal.valueOf(90.00),
                "Score de teste",
                "oprm-cnae-score-v1",
                "OPRM-CNAE-SCORE-20260601-001",
                Instant.parse("2026-06-01T03:00:00Z"),
                "SCORED",
                null);
    }
}
