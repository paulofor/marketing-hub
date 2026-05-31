package com.marketinghub.geralanding.imagegeneration.service.pending;

/** Representa os dados do experimento expostos na fila interna da etapa image generation. */
public record RecordImageGenerationExperiment(
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
