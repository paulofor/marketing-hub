package com.marketinghub.productdiscovery.v1.service;

import jakarta.validation.constraints.NotBlank;

/** Responsabilidade: transportar o plano auditável de pesquisa dirigida criado por Argos. */
public record ProductDiscoveryResearchPlanRequest(
    @NotBlank String planJson, @NotBlank String rawResponse, @NotBlank String model) {}
