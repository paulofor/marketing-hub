package com.marketinghub.productdiscovery.v1.service;

import java.util.List;

/** Agrupa ofertas de marketplace com a consulta e a proveniencia da coleta. */
public record ProductDiscoveryMarketplaceOfferListResponse(
    String marketplace,
    String query,
    String collectionJobId,
    List<ProductDiscoveryMarketplaceOfferResponse> items) {}
