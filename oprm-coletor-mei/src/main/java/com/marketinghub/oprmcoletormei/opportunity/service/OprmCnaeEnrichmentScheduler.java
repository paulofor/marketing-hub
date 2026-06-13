package com.marketinghub.oprmcoletormei.opportunity.service;

import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeCycleUpsertRequestDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityScoreResponseDto;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Executor manual responsável por enriquecer CNAEs com melhor score e publicar candidatos de nicho, sem ciclos automáticos.
 */
@Component
@ConditionalOnProperty(name = "oprm.cnae-enrichment.scheduler.enabled", havingValue = "true")
public class OprmCnaeEnrichmentScheduler {
    private static final Logger log = LoggerFactory.getLogger(OprmCnaeEnrichmentScheduler.class);
    private static final String CYCLE_TYPE = "CNAE_ENRICHMENT";
    private static final int BATCH_LIMIT = 25;

    private final OprmCnaeOpportunityBackendClient backendClient;
    private final OprmCnaeRoutineSignalBuilder routineSignalBuilder;

    /** Inicializa o executor manual com o cliente de APIs OPRM do backend e o builder de sinais de rotina. */
    public OprmCnaeEnrichmentScheduler(
            OprmCnaeOpportunityBackendClient backendClient,
            OprmCnaeRoutineSignalBuilder routineSignalBuilder) {
        this.backendClient = backendClient;
        this.routineSignalBuilder = routineSignalBuilder;
    }

    /** Executa manualmente o ciclo de enriquecimento para CNAEs priorizados por score. */
    public void runEnrichmentCycle() {
        Long cycleNumber = null;
        String cycleId = "OPRM-CNAE-ENRICHMENT-UNRESOLVED";
        Instant startedAt = Instant.now();
        int processed = 0;
        int failed = 0;
        try {
            cycleNumber = backendClient.nextCycleNumber(CYCLE_TYPE);
            cycleId = buildCycleId(CYCLE_TYPE, cycleNumber);
            log.info("[OPRM-CNAE-ENRICHMENT] Iniciando ciclo. cycleId={} cycleType={} cycleNumber={} limit={}", cycleId, CYCLE_TYPE, cycleNumber, BATCH_LIMIT);
            backendClient.upsertCycle(new OprmCnaeCycleUpsertRequestDto(
                    cycleId, CYCLE_TYPE, cycleNumber, "RUNNING", "top scores não enriquecidos; limit=" + BATCH_LIMIT, 0, 0, startedAt, null, null, null));
            List<OprmCnaeOpportunityScoreResponseDto> scores = backendClient.findTopScores(BATCH_LIMIT);
            log.info("[OPRM-CNAE-ENRICHMENT] CNAEs priorizados carregados. cycleId={} total={}", cycleId, scores.size());
            for (OprmCnaeOpportunityScoreResponseDto score : scores) {
                try {
                    backendClient.saveEnrichment(routineSignalBuilder.buildEnrichment(score, cycleId));
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
            markCycleAsFailedWhenPossible(cycleId, cycleNumber, processed, failed, startedAt, ex);
        }
    }

    /** Registra falha de ciclo quando já existe número de ciclo disponível para persistência no backend. */
    private void markCycleAsFailedWhenPossible(String cycleId, Long cycleNumber, int processed, int failed, Instant startedAt, RuntimeException ex) {
        if (cycleNumber == null) {
            return;
        }
        try {
            backendClient.upsertCycle(new OprmCnaeCycleUpsertRequestDto(
                    cycleId, CYCLE_TYPE, cycleNumber, "FAILED", "top scores não enriquecidos; limit=" + BATCH_LIMIT,
                    processed, failed, startedAt, Instant.now(), null, ex.getMessage()));
        } catch (RuntimeException persistEx) {
            log.error("[OPRM-CNAE-ENRICHMENT] Falha ao persistir status FAILED do ciclo. cycleId={} cycleType={} cycleNumber={}",
                    cycleId, CYCLE_TYPE, cycleNumber, persistEx);
        }
    }

    /** Cria identificador legível do ciclo para logs e comunicação operacional. */
    private String buildCycleId(String cycleType, Long cycleNumber) {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(Instant.now());
        return "OPRM-" + cycleType.replace('_', '-') + "-" + date + "-" + String.format("%03d", cycleNumber);
    }
}
