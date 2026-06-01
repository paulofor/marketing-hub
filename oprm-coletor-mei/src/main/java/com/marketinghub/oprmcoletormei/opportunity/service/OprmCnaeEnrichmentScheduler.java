package com.marketinghub.oprmcoletormei.opportunity.service;

import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeCycleUpsertRequestDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeEnrichmentRequestDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityScoreResponseDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmNicheCandidateRequestDto;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler responsável por enriquecer automaticamente CNAEs com melhor score e publicar candidatos de nicho.
 */
@Component
public class OprmCnaeEnrichmentScheduler {
    private static final Logger log = LoggerFactory.getLogger(OprmCnaeEnrichmentScheduler.class);
    private static final String CYCLE_TYPE = "CNAE_ENRICHMENT";
    private static final int BATCH_LIMIT = 25;

    private final OprmCnaeOpportunityBackendClient backendClient;

    /** Inicializa o scheduler com o cliente de APIs OPRM do backend. */
    public OprmCnaeEnrichmentScheduler(OprmCnaeOpportunityBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Executa periodicamente o ciclo de enriquecimento para CNAEs priorizados por score já calculado. */
    @Scheduled(cron = "0 15 * * * *", zone = "America/Sao_Paulo")
    public void runEnrichmentCycle() {
        Long cycleNumber = backendClient.nextCycleNumber(CYCLE_TYPE);
        String cycleId = buildCycleId(CYCLE_TYPE, cycleNumber);
        Instant startedAt = Instant.now();
        int processed = 0;
        int failed = 0;
        log.info("[OPRM-CNAE-ENRICHMENT] Iniciando ciclo. cycleId={} cycleType={} cycleNumber={} limit={}", cycleId, CYCLE_TYPE, cycleNumber, BATCH_LIMIT);
        backendClient.upsertCycle(new OprmCnaeCycleUpsertRequestDto(
                cycleId, CYCLE_TYPE, cycleNumber, "RUNNING", "top scores não enriquecidos; limit=" + BATCH_LIMIT, 0, 0, startedAt, null, null, null));
        try {
            List<OprmCnaeOpportunityScoreResponseDto> scores = backendClient.findTopScores(BATCH_LIMIT);
            log.info("[OPRM-CNAE-ENRICHMENT] CNAEs priorizados carregados. cycleId={} total={}", cycleId, scores.size());
            for (OprmCnaeOpportunityScoreResponseDto score : scores) {
                try {
                    backendClient.saveEnrichment(buildEnrichment(score, cycleId));
                    processed++;
                    log.info("[OPRM-CNAE-ENRICHMENT] Enriquecimento gravado. cycleId={} cnaeCode={} processed={}", cycleId, score.cnaeCode(), processed);
                } catch (RuntimeException ex) {
                    failed++;
                    log.error("[OPRM-CNAE-ENRICHMENT] Falha ao enriquecer CNAE. cycleId={} cycleType={} cycleNumber={} cnaeCode={} processed={} failed={}",
                            cycleId, CYCLE_TYPE, cycleNumber, score.cnaeCode(), processed, failed, ex);
                }
            }
            backendClient.upsertCycle(new OprmCnaeCycleUpsertRequestDto(
                    cycleId, CYCLE_TYPE, cycleNumber, failed > 0 ? "PARTIAL" : "COMPLETED", "top scores não enriquecidos; limit=" + BATCH_LIMIT,
                    processed, failed, startedAt, Instant.now(), "Ciclo de enriquecimento finalizado pelo OPRM.", null));
            log.info("[OPRM-CNAE-ENRICHMENT] Ciclo finalizado. cycleId={} processed={} failed={}", cycleId, processed, failed);
        } catch (RuntimeException ex) {
            log.error("[OPRM-CNAE-ENRICHMENT] Falha crítica no ciclo. cycleId={} cycleType={} cycleNumber={} processed={} failed={}",
                    cycleId, CYCLE_TYPE, cycleNumber, processed, failed, ex);
            backendClient.upsertCycle(new OprmCnaeCycleUpsertRequestDto(
                    cycleId, CYCLE_TYPE, cycleNumber, "FAILED", "top scores não enriquecidos; limit=" + BATCH_LIMIT,
                    processed, failed, startedAt, Instant.now(), null, ex.getMessage()));
        }
    }

    /** Monta enriquecimento inicial estruturado pelo eixo Dor Resultado Mecanismo Prova Oferta. */
    private OprmCnaeEnrichmentRequestDto buildEnrichment(OprmCnaeOpportunityScoreResponseDto score, String cycleId) {
        String persona = "Profissional ou pequeno negócio vinculado ao CNAE " + score.cnaeDescription();
        String pain = "Dificuldade de transformar rotina operacional em aquisição previsível de clientes e vendas recorrentes.";
        String outcome = "Aumentar clareza de oferta, captação local e previsibilidade comercial com menor esforço operacional.";
        String mechanism = "Playbook digital com diagnóstico de rotina, mensagens prontas, calendário de ações e uso prático de IA para reduzir esforço.";
        String proof = "Validar com evidências de rotina OPRM, métricas de volume CNAE/MEI e experimento pequeno antes de criar produto final.";
        String offer = "Kit de crescimento comercial com IA para " + score.cnaeDescription();
        String marketSignals = "opportunityScore=" + score.opportunityScore() + "; algorithmVersion=" + score.algorithmVersion();
        OprmNicheCandidateRequestDto candidate = new OprmNicheCandidateRequestDto(
                score.cnaeCode(),
                score.cnaeDescription(),
                "IA para crescimento de " + score.cnaeDescription(),
                persona,
                pain,
                outcome,
                mechanism,
                proof,
                offer,
                marketSignals,
                score.opportunityScore(),
                score.cycleId(),
                cycleId,
                "ENRICHED",
                "cycleId=" + cycleId + "; scoreCycleId=" + score.cycleId());
        return new OprmCnaeEnrichmentRequestDto(
                score.cnaeCode(),
                cycleId,
                "Rotina a pesquisar: atendimento, aquisição de clientes, entrega, cobrança e recorrência para " + score.cnaeDescription(),
                pain,
                mechanism,
                proof,
                offer,
                "Enriquecimento inicial OPRM estruturado para orientar pesquisa externa posterior e decisão humana.",
                List.of(candidate));
    }

    /** Cria identificador legível do ciclo para logs e comunicação operacional. */
    private String buildCycleId(String cycleType, Long cycleNumber) {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(Instant.now());
        return "OPRM-" + cycleType.replace('_', '-') + "-" + date + "-" + String.format("%03d", cycleNumber);
    }
}
