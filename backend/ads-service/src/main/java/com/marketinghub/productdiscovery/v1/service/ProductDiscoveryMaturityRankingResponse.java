package com.marketinghub.productdiscovery.v1.service;

import java.util.List;

/** Contrato do painel gerencial de ranking por maturidade comercial de oportunidades PDE. */
public record ProductDiscoveryMaturityRankingResponse(
        String strategyName,
        String decisionCriterion,
        String recommendedPriority,
        List<ProductDiscoveryMaturityItemResponse> items,
        List<ProductDiscoveryResearchTrackResponse> recommendedTracks) {}
