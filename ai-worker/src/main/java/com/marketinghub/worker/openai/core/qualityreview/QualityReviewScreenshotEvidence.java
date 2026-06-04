package com.marketinghub.worker.openai.core.qualityreview;

/** Responsabilidade: transportar a evidência auditável de um screenshot renderizado no Quality Review. */
public record QualityReviewScreenshotEvidence(
        String viewport,
        String publicUrl,
        String sha256,
        int bytes
) {
    /** Mantém os dados imutáveis do screenshot enviado ao modelo de visão. */
    public QualityReviewScreenshotEvidence {}
}
