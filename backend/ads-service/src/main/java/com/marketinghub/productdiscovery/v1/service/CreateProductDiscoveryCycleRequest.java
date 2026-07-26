package com.marketinghub.productdiscovery.v1.service;

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
        String objective) {}
