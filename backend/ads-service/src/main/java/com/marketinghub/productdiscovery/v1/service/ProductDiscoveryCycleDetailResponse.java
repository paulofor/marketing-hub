package com.marketinghub.productdiscovery.v1.service;

import java.util.List;

/** Resposta detalhada de um ciclo com oportunidades descobertas. */
public record ProductDiscoveryCycleDetailResponse(
        ProductDiscoveryCycleResponse cycle,
        List<ProductDiscoveryOpportunityResponse> opportunities) {}
