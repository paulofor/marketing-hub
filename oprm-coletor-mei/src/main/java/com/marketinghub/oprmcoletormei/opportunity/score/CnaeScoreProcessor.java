package com.marketinghub.oprmcoletormei.opportunity.score;

import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityCandidateDto;
import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityScoreRequestDto;
import com.marketinghub.oprmcoletormei.opportunity.pipeline.StageArtifact;
import com.marketinghub.oprmcoletormei.opportunity.pipeline.StageContext;
import com.marketinghub.oprmcoletormei.opportunity.pipeline.StageProcessor;
import com.marketinghub.oprmcoletormei.opportunity.pipeline.StageResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Etapa concreta responsável por calcular score OPRM determinístico a partir de métricas CNAE recebidas do backend. */
public class CnaeScoreProcessor implements StageProcessor<CnaeScoreInput, CnaeScoreOutput> {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MARKET_NORMALIZER = BigDecimal.valueOf(1_000_000L);
    private static final String ALGORITHM_VERSION = "oprm-cnae-score-v1";

    /** Calcula componentes do score e retorna o DTO oficial de persistência no backend. */
    @Override
    public StageResult<CnaeScoreOutput> process(StageContext<CnaeScoreInput> context) {
        OprmCnaeOpportunityCandidateDto candidate = context.input().candidate();
        BigDecimal marketVolumeScore = boundedRatio(candidate.totalEstabelecimentosAtivos(), MARKET_NORMALIZER);
        BigDecimal meiDensityScore = percentage(candidate.totalEmpresasMei(), candidate.totalEmpresas());
        BigDecimal digitalFitScore = digitalFitScore(candidate.cnaeDescription());
        BigDecimal painClarityScore = painClarityScore(candidate.cnaeDescription());
        BigDecimal opportunityScore = marketVolumeScore.multiply(BigDecimal.valueOf(0.35))
                .add(meiDensityScore.multiply(BigDecimal.valueOf(0.25)))
                .add(digitalFitScore.multiply(BigDecimal.valueOf(0.20)))
                .add(painClarityScore.multiply(BigDecimal.valueOf(0.20)))
                .setScale(2, RoundingMode.HALF_UP);
        OprmCnaeOpportunityScoreRequestDto request = new OprmCnaeOpportunityScoreRequestDto(
                candidate.cnaeDescription(),
                opportunityScore,
                marketVolumeScore,
                meiDensityScore,
                digitalFitScore,
                painClarityScore,
                buildJustification(candidate, opportunityScore),
                ALGORITHM_VERSION,
                context.cycleId(),
                Instant.now(),
                "SCORED");
        StageArtifact artifact = new StageArtifact(
                "NORMALIZED_JSON",
                "cnae-score-" + candidate.cnaeCode(),
                "application/json",
                "opportunity/score/" + context.cycleId() + "/" + candidate.cnaeCode(),
                null,
                Map.of("cnaeCode", candidate.cnaeCode(), "algorithmVersion", ALGORITHM_VERSION));
        return new StageResult<>(
                new CnaeScoreOutput(candidate.cnaeCode(), request),
                List.of(context.artifactStore().store(artifact)),
                Map.of("opportunityScore", opportunityScore, "stageName", context.stageName()));
    }

    /** Calcula percentual limitado a 100 para componentes proporcionais do score. */
    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal value = BigDecimal.valueOf(numerator)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
        return value.min(HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }

    /** Calcula proporção limitada a 100 usando normalizador de mercado. */
    private BigDecimal boundedRatio(long value, BigDecimal normalizer) {
        return BigDecimal.valueOf(value)
                .multiply(HUNDRED)
                .divide(normalizer, 2, RoundingMode.HALF_UP)
                .min(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Estima aderência digital por palavras de rotina comercial recorrente do CNAE. */
    private BigDecimal digitalFitScore(String description) {
        String text = normalize(description);
        if (containsAny(text, "cabeleireiro", "comercio", "promocao", "documentos", "servicos", "treinamento")) {
            return BigDecimal.valueOf(85).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(65).setScale(2, RoundingMode.HALF_UP);
    }

    /** Estima clareza de dor operacional por termos de serviço, rotina ou venda local. */
    private BigDecimal painClarityScore(String description) {
        String text = normalize(description);
        if (containsAny(text, "servicos", "obra", "manutencao", "beleza", "varejista", "alimentacao")) {
            return BigDecimal.valueOf(90).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(70).setScale(2, RoundingMode.HALF_UP);
    }

    /** Monta justificativa objetiva para auditoria do score gravado. */
    private String buildJustification(OprmCnaeOpportunityCandidateDto candidate, BigDecimal opportunityScore) {
        return "Score OPRM=" + opportunityScore
                + "; ativos=" + candidate.totalEstabelecimentosAtivos()
                + "; empresasMei=" + candidate.totalEmpresasMei()
                + "; descrição=" + candidate.cnaeDescription();
    }

    /** Verifica se algum termo de referência aparece no texto normalizado. */
    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    /** Normaliza a descrição do CNAE para comparações determinísticas simples. */
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
