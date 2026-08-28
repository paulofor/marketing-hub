package com.marketinghub.pde.dto;

import java.util.List;

/** Responsabilidade: declarar rotas, eventos e correlações preparados para homologação do PDE. */
public record ProductJourneyIntegrationContractResponse(
        String productSlug,
        String experienceVersion,
        String contractVersion,
        String eventsPath,
        String analyticsSummaryPath,
        String loginPath,
        String workspacePathTemplate,
        String missionCompletionPathTemplate,
        List<String> requiredEventTypes,
        List<String> correlationKeys,
        String sourceOfTruth,
        String testTrafficPolicy) {}
