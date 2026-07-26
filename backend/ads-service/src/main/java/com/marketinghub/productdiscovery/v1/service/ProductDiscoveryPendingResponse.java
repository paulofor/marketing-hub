package com.marketinghub.productdiscovery.v1.service;

/** Contrato de pendência que o worker consome para pesquisar oportunidades PDE. */
public record ProductDiscoveryPendingResponse(
        Long cycleId,
        String pipelineCode,
        String stageCode,
        String theme,
        String targetAudience,
        String country,
        String language,
        String acquisitionChannel,
        String commercialConstraints,
        String forbiddenCategories,
        String objective) {}
