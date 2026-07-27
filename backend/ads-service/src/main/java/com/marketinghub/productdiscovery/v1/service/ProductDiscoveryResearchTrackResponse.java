package com.marketinghub.productdiscovery.v1.service;

/** Contrato de uma trilha recomendada para abrir novo ciclo de descoberta PDE. */
public record ProductDiscoveryResearchTrackResponse(
    String name,
    String focus,
    String reason,
    String theme,
    String targetAudience,
    String acquisitionChannel,
    String objective,
    String commercialConstraints,
    String forbiddenCategories) {}
