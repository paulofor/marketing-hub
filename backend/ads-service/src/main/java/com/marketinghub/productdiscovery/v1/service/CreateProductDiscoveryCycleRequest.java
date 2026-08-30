package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryMarketType;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryResearchMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contrato de criação de ciclo de descoberta de produtos PDE. */
public record CreateProductDiscoveryCycleRequest(
    @NotBlank @Size(max = 191) String theme,
    @Size(max = 191) String targetAudience,
    @Size(max = 16) String country,
    @Size(max = 16) String language,
    @Size(max = 120) String acquisitionChannel,
    String commercialConstraints,
    String forbiddenCategories,
    String objective,
    ProductDiscoveryResearchMode researchMode,
    ProductDiscoveryMarketType marketType,
    @Size(max = 5000) String referenceSources) {

  /** Mantém compatibilidade com clientes que validam um mercado pelo briefing histórico. */
  public CreateProductDiscoveryCycleRequest(
      String theme,
      String targetAudience,
      String country,
      String language,
      String acquisitionChannel,
      String commercialConstraints,
      String forbiddenCategories,
      String objective) {
    this(
        theme,
        targetAudience,
        country,
        language,
        acquisitionChannel,
        commercialConstraints,
        forbiddenCategories,
        objective,
        null,
        null,
        null);
  }
}
