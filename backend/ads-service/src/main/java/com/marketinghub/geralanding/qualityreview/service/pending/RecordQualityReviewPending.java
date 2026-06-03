package com.marketinghub.geralanding.qualityreview.service.pending;

import java.time.Instant;

/** Representa um job pendente da etapa quality-review consumido pelo Worker AI. */
public record RecordQualityReviewPending(
        Long experimentId,
        String jobid,
        String stageCode,
        Instant executionRequestedAt,
        RecordQualityReviewExperiment experiment,
        RecordQualityReviewHypothesis hypothesis
) {
    /** Mantém o contrato imutável do item pendente da revisão visual. */
    public RecordQualityReviewPending {}
}
