package com.marketinghub.productdiscovery.v1.service;

import java.time.Instant;
import java.util.List;

/** Expõe a sessão supervisionada que complementa uma execução factual de Argos. */
public record ProductDiscoverySupervisedMetaSessionResponse(
    Long cycleId,
    Long investigationId,
    String cycleStatus,
    String query,
    String country,
    String publisherPlatform,
    String sourceStatus,
    String collectionMode,
    String collectionReason,
    String searchUrl,
    Instant nextObservationAt,
    int adsObserved,
    int activeAds,
    int advertisersObserved,
    Instant latestObservationAt,
    String interpretation,
    boolean canRegisterObservation,
    boolean canResume,
    String resumeReason,
    List<ProductDiscoveryMetaAdEvidenceResponse> items) {}
