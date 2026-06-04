package com.marketinghub.geralanding.qualityreview.service.listStageExecutions;

import java.math.BigDecimal;
import java.time.Instant;

/** Resumo de uma execução da revisão de qualidade da landing. */
public record GeraLandingQualityReviewExecutionSummaryResponse(
        String idJob,
        String status,
        Instant executionRequestedAt,
        BigDecimal costUsd,
        BigDecimal score,
        String approvalRecommendation,
        Boolean approvedForPublication
) {
    /** Mantém o contrato imutável do resumo de execução. */
    public GeraLandingQualityReviewExecutionSummaryResponse {}
}
