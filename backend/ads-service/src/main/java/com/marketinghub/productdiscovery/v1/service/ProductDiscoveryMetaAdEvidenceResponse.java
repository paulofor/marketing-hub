package com.marketinghub.productdiscovery.v1.service;

import java.time.Instant;

/** Representa um sinal comercial auditável observado na Biblioteca de Anúncios da Meta. */
public record ProductDiscoveryMetaAdEvidenceResponse(
    String metaAdId,
    String advertiserName,
    String adText,
    String formatTypes,
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
