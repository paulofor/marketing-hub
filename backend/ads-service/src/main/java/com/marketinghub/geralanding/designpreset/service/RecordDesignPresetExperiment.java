package com.marketinghub.geralanding.designpreset.service;

/** Representa os dados do experimento expostos na fila interna da etapa design preset. */
public record RecordDesignPresetExperiment(
        Long id,
        String name,
        String hypothesis,
        String status,
        String stage,
        String creativeTextPrompt,
        String creativeImagePrompt,
        Object campaignAngle,
        Object adCopy,
        Object adImageBriefing,
        Object landingPageCopy,
        Object landingPageWireframe,
        Object landingPageImagePlanning,
        Object landingPageDesignPreset,
        Object landingPageDeliverables,
        String htmlGeraLanding
) {
}
