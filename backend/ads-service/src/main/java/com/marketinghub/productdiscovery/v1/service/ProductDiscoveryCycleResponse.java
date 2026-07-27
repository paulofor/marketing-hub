package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import java.time.Instant;

/** Resposta resumida de um ciclo de descoberta de produtos PDE. */
public record ProductDiscoveryCycleResponse(
    Long id,
    String theme,
    String targetAudience,
    String country,
    String language,
    String acquisitionChannel,
    ProductDiscoveryCycleStatus status,
    String stageCode,
    String decisionSummary,
    String errorMessage,
    Instant createdAt,
    Instant updatedAt) {}
