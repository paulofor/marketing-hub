package com.marketinghub.geralanding.presetdesign.service.pending;

/** Representa os dados do experimento expostos na fila interna da etapa preset design. */
public record RecordPresetDesignExperiment(
        Long id,
        String name,
        String hypothesis,
        String status,
        String stage,
        String creativeTextPrompt,
        String creativeImagePrompt,
        String singlePain,
        String freeReward,
        String funnelPromise,
        String primaryCta,
        String campaignObjective,
        Object campaignAngle,
        Object adCopy,
        Object adImageBriefing,
        Object landingPageCopy,
        Object landingPageWireframe,
        Object landingPageImagePlanning,
        Object landingPageDesignPreset,
        Object landingPageQualityReview,
        Object landingPageDeliverables,
        String htmlGeraLanding
) {
}
