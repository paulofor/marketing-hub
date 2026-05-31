package com.marketinghub.geralanding.imageplanning.service.pending;

/** Representa os dados do experimento expostos na fila interna da etapa image planning. */
public record RecordImagePlanningExperiment(
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
