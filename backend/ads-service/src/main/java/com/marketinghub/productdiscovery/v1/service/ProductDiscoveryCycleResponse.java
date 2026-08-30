package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryMarketType;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryResearchMode;
import java.time.Instant;

/** Resposta resumida de um ciclo de descoberta de produtos PDE. */
public record ProductDiscoveryCycleResponse(
    Long id,
    String theme,
    String targetAudience,
    String country,
    String language,
    String acquisitionChannel,
    ProductDiscoveryResearchMode researchMode,
    ProductDiscoveryMarketType marketType,
    String referenceSources,
    ProductDiscoveryCycleStatus status,
    String stageCode,
    String decisionSummary,
    String errorMessage,
    Instant createdAt,
    Instant updatedAt) {

  /** Mantém compatibilidade com respostas construídas antes do modo explícito de pesquisa. */
  public ProductDiscoveryCycleResponse(
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
      Instant updatedAt) {
    this(
        id,
        theme,
        targetAudience,
        country,
        language,
        acquisitionChannel,
        ProductDiscoveryResearchMode.VALIDATE_MARKET,
        ProductDiscoveryMarketType.UNSPECIFIED,
        null,
        status,
        stageCode,
        decisionSummary,
        errorMessage,
        createdAt,
        updatedAt);
  }
}
