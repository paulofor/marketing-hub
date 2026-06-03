package com.marketinghub.geralanding.qualityreview.service;

/** Resposta do início síncrono da etapa de revisão de qualidade da landing. */
public record GeraLandingQualityReviewStartResponse(
        String idJob,
        String status,
        String qualityReview
) {
    /** Mantém o contrato imutável da resposta da etapa. */
    public GeraLandingQualityReviewStartResponse {}
}
