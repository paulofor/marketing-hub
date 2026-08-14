package com.marketinghub.productdiscovery.v1.service;

import java.util.List;

/** Consolida anúncios Meta encontrados para uma pergunta dirigida de Argos. */
public record ProductDiscoveryMetaAdEvidenceListResponse(
    String query,
    String country,
    String interpretation,
    List<ProductDiscoveryMetaAdEvidenceResponse> items) {}
