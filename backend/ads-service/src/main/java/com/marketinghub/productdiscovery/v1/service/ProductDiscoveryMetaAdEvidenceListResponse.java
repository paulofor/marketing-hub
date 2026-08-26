package com.marketinghub.productdiscovery.v1.service;

import java.time.Instant;
import java.util.List;

/** Consolida anúncios Meta encontrados para uma pergunta dirigida de Argos. */
public record ProductDiscoveryMetaAdEvidenceListResponse(
    Long cycleId,
    String query,
    String country,
    String publisherPlatform,
    String sourceStatus,
    String collectionMode,
    Long investigationId,
    String searchUrl,
    int adsObserved,
    int activeAds,
    int advertisersObserved,
    Instant latestObservationAt,
    String interpretation,
    List<ProductDiscoveryMetaAdEvidenceResponse> items) {}
