package com.marketinghub.productdiscovery.v1.service;

import java.time.Instant;
import java.util.List;

/** Representa um sinal comercial auditável observado na Biblioteca de Anúncios da Meta. */
public record ProductDiscoveryMetaAdEvidenceResponse(
    String metaAdId,
    String advertiserName,
    List<String> adTexts,
    List<String> publisherPlatforms,
    List<String> formatTypes,
    String destinationUrl,
    String snapshotUrl,
    boolean active,
    boolean commercialSignal,
    int observations,
    long longevityDays,
    boolean sustainedInvestmentSignal,
    String evidenceConfidence,
    Instant firstObservedAt,
    Instant lastObservedAt) {}
