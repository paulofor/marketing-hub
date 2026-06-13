package com.marketinghub.oprmcoletormei.opportunity.service;

import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeCycleUpsertRequestDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityCandidateDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityScoreRequestDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler responsável por calcular scores de oportunidade para CNAEs ainda não pontuados quando habilitado operacionalmente.
 */
@Component
@ConditionalOnProperty(name = "oprm.cnae-opportunity.scheduler.enabled", havingValue = "true")
public class OprmCnaeOpportunityScheduler {
    private static final Logger log = LoggerFactory.getLogger(OprmCnaeOpportunityScheduler.class);
    private static final String CYCLE_TYPE = "CNAE_SCORE";
    private static final String ALGORITHM_VERSION = "oprm-cnae-score-v1";
    private static final int BATCH_LIMIT = 50;

    private final OprmCnaeOpportunityBackendClient backendClient;

    /** Inicializa o scheduler com o cliente de APIs OPRM do backend. */
    public OprmCnaeOpportunityScheduler(OprmCnaeOpportunityBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /** Executa o ciclo agendado de score para CNAEs sem score quando o scheduler estiver habilitado. */
    @Scheduled(cron = "0 */30 * * * *", zone = "America/Sao_Paulo")
    public void runScoreCycle() {
        Long cycleNumber = backendClient.nextCycleNumber(CYCLE_TYPE);
        String cycleId = buildCycleId(CYCLE_TYPE, cycleNumber);
        Instant startedAt = Instant.now();
        int processed = 0;
        int failed = 0;
        log.info("[OPRM-CNAE-SCORE] Iniciando ciclo. cycleId={} cycleType={} cycleNumber={} limit={}", cycleId, CYCLE_TYPE, cycleNumber, BATCH_LIMIT);
        backendClient.upsertCycle(new OprmCnaeCycleUpsertRequestDto(
                cycleId, CYCLE_TYPE, cycleNumber, "RUNNING", "score ausente; limit=" + BATCH_LIMIT, 0, 0, startedAt, null, null, null));
        try {
            List<OprmCnaeOpportunityCandidateDto> candidates = backendClient.findMissingScores(BATCH_LIMIT);
            log.info("[OPRM-CNAE-SCORE] CNAEs sem score carregados. cycleId={} total={}", cycleId, candidates.size());
            for (OprmCnaeOpportunityCandidateDto candidate : candidates) {
                try {
                    backendClient.saveScore(candidate.cnaeCode(), buildScore(candidate, cycleId));
                    processed++;
                    log.info("[OPRM-CNAE-SCORE] Score gravado. cycleId={} cnaeCode={} processed={}", cycleId, candidate.cnaeCode(), processed);
                } catch (RuntimeException ex) {
                    failed++;
                    log.error("[OPRM-CNAE-SCORE] Falha ao calcular/gravar score. cycleId={} cycleType={} cycleNumber={} cnaeCode={} processed={} failed={}",
                            cycleId, CYCLE_TYPE, cycleNumber, candidate.cnaeCode(), processed, failed, ex);
                }
            }
            backendClient.upsertCycle(new OprmCnaeCycleUpsertRequestDto(
                    cycleId, CYCLE_TYPE, cycleNumber, failed > 0 ? "PARTIAL" : "COMPLETED", "score ausente; limit=" + BATCH_LIMIT,
                    processed, failed, startedAt, Instant.now(), "Ciclo de score finalizado pelo OPRM.", null));
            log.info("[OPRM-CNAE-SCORE] Ciclo finalizado. cycleId={} processed={} failed={}", cycleId, processed, failed);
        } catch (RuntimeException ex) {
            log.error("[OPRM-CNAE-SCORE] Falha crítica no ciclo. cycleId={} cycleType={} cycleNumber={} processed={} failed={}",
                    cycleId, CYCLE_TYPE, cycleNumber, processed, failed, ex);
            backendClient.upsertCycle(new OprmCnaeCycleUpsertRequestDto(
                    cycleId, CYCLE_TYPE, cycleNumber, "FAILED", "score ausente; limit=" + BATCH_LIMIT,
                    processed, failed, startedAt, Instant.now(), null, ex.getMessage()));
        }
    }

    /** Monta o payload de score usando regra OPRM simples e determinística. */
    private OprmCnaeOpportunityScoreRequestDto buildScore(OprmCnaeOpportunityCandidateDto candidate, String cycleId) {
        BigDecimal marketVolumeScore = cap(candidate.totalEmpresasMei(), 600_000L);
        BigDecimal meiDensityScore = ratio(candidate.totalEmpresasMei(), Math.max(candidate.totalEmpresas(), 1L));
        BigDecimal digitalFitScore = keywordScore(candidate.cnaeDescription());
        BigDecimal painClarityScore = painScore(candidate.cnaeDescription());
        BigDecimal opportunityScore = marketVolumeScore.multiply(BigDecimal.valueOf(0.35))
                .add(meiDensityScore.multiply(BigDecimal.valueOf(0.25)))
                .add(digitalFitScore.multiply(BigDecimal.valueOf(0.20)))
                .add(painClarityScore.multiply(BigDecimal.valueOf(0.20)))
                .setScale(2, RoundingMode.HALF_UP);
        String justification = "Score OPRM calculado com volume MEI, densidade MEI, aderência a produto digital e clareza provável de dor operacional/comercial.";
        return new OprmCnaeOpportunityScoreRequestDto(
                candidate.cnaeDescription(), opportunityScore, marketVolumeScore, meiDensityScore, digitalFitScore, painClarityScore,
                justification, ALGORITHM_VERSION, cycleId, Instant.now(), "SCORED");
    }

    /** Calcula pontuação proporcional limitada a 100 para volumes de mercado. */
    private BigDecimal cap(long value, long maxReference) {
        return BigDecimal.valueOf(Math.min(100.0, (value * 100.0) / maxReference)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Calcula pontuação proporcional percentual limitada a 100. */
    private BigDecimal ratio(long numerator, long denominator) {
        return BigDecimal.valueOf(Math.min(100.0, (numerator * 100.0) / denominator)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Estima aderência inicial a produto digital pela linguagem do CNAE. */
    private BigDecimal keywordScore(String description) {
        String text = normalize(description);
        if (text.contains("servico") || text.contains("educacao") || text.contains("treinamento") || text.contains("beleza") || text.contains("administrativo")) {
            return BigDecimal.valueOf(85).setScale(2, RoundingMode.HALF_UP);
        }
        if (text.contains("comercio") || text.contains("varejista") || text.contains("promocao")) {
            return BigDecimal.valueOf(75).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(60).setScale(2, RoundingMode.HALF_UP);
    }

    /** Estima clareza provável de dor comercial ou operacional pela descrição do CNAE. */
    private BigDecimal painScore(String description) {
        String text = normalize(description);
        if (text.contains("cabeleireiro") || text.contains("manicure") || text.contains("obras") || text.contains("vendas") || text.contains("administrativo")) {
            return BigDecimal.valueOf(90).setScale(2, RoundingMode.HALF_UP);
        }
        if (text.contains("comercio") || text.contains("servico")) {
            return BigDecimal.valueOf(80).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(65).setScale(2, RoundingMode.HALF_UP);
    }

    /** Normaliza texto simples para comparação de palavras-chave. */
    private String normalize(String value) {
        return value == null ? "" : java.text.Normalizer.normalize(value.toLowerCase(), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** Cria identificador legível do ciclo para logs e comunicação operacional. */
    private String buildCycleId(String cycleType, Long cycleNumber) {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(Instant.now());
        return "OPRM-" + cycleType.replace('_', '-') + "-" + date + "-" + String.format("%03d", cycleNumber);
    }
}
