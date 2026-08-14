package com.marketinghub.productdiscovery.v1.service;

import java.time.Instant;

/** Representa uma oferta coletada e normalizada para a investigacao comercial de Argos. */
public record ProductDiscoveryMarketplaceOfferResponse(
    String marketplace,
    String referenceId,
    String title,
    String productUrl,
    String description,
    String producer,
    String price,
    Double tractionSignal,
    Double rating,
    Integer reviewCount,
    Double blueprint,
    String commission,
    String category,
    String format,
    Integer rankingPosition,
    Integer observations,
    Double previousTractionSignal,
    Instant firstObservedAt,
    Instant collectedAt,
    String evidenceConfidence) {}
