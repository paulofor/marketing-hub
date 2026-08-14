package com.marketinghub.productdiscovery.v1.service;

import java.time.Instant;

/** Responsabilidade: expor a proveniência do plano dirigido sem revelar credenciais. */
public record ProductDiscoveryResearchPlanResponse(
    Long cycleId, String planJson, String model, Instant updatedAt) {}
