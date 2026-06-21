package com.marketinghub.geralanding.qualityreview.service.pending;

import java.time.Instant;

/** Representa um job pendente da etapa quality-review consumido pelo Worker AI. */
public record RecordQualityReviewPending(
        Long experimentId,
        String jobid,
        String stageCode,
        Instant executionRequestedAt,
        String experimentName,
        String hypothesisTitle,
        String singlePain,
        String freeReward,
        String funnelPromise,
        String primaryCta,
        String campaignObjective,
        Object landingPageWireframe,
        Object landingPageDesignPreset,
        String htmlGeraLanding
) {
    /** Mantém o contrato imutável do item pendente da revisão visual. */
    public RecordQualityReviewPending {}
}
