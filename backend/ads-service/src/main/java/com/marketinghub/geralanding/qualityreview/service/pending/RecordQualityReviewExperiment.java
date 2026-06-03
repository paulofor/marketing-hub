package com.marketinghub.geralanding.qualityreview.service.pending;

/** Representa os artefatos do experimento expostos para a revisão visual de qualidade da landing. */
public record RecordQualityReviewExperiment(
        Long id,
        String name,
        String hypothesis,
        String status,
        String stage,
        Object campaignAngle,
        Object adCopy,
        Object adImageBriefing,
        Object landingPageCopy,
        Object landingPageWireframe,
        Object landingPageImagePlanning,
        Object landingPageImageAssets,
        Object landingPageDesignPreset,
        Object landingPageDeliverables,
        String htmlGeraLanding,
        String landingPageHtml
) {
    /** Mantém o contrato imutável dos dados do experimento para o pending da revisão visual. */
    public RecordQualityReviewExperiment {}
}
