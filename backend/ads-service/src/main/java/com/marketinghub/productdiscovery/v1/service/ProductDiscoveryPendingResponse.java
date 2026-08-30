package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryMarketType;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryResearchMode;

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
    String objective,
    ProductDiscoveryResearchMode researchMode,
    ProductDiscoveryMarketType marketType,
    String referenceSources,
    String executionLeaseId,
    int executionAttempt) {}
