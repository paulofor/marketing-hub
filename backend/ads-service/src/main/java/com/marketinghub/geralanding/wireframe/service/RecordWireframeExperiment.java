package com.marketinghub.geralanding.wireframe.service;

/** Representa os dados do experimento expostos na fila interna da etapa wireframe. */
public record RecordWireframeExperiment(
        Long id,
        String name,
        String hypothesis,
        String status,
        String stage,
        String creativeTextPrompt,
        String creativeImagePrompt,
        String campaignAngle,
        String adCopy,
        String adImageBriefing,
        String landingPageCopy,
        String landingPageWireframe,
        String landingPageImagePlanning,
        String landingPageDesignPreset,
        String landingPageDeliverables,
        String htmlGeraLanding
) {
}
