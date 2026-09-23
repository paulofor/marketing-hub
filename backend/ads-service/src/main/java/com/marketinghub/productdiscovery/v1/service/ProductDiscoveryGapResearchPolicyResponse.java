package com.marketinghub.productdiscovery.v1.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Limites objetivos que impedem aprofundamento aberto e consumo de busca sem teto. */
public record ProductDiscoveryGapResearchPolicyResponse(
    int maximumAttempts,
    int maximumPublicQueriesPerAttempt,
    int maximumModelInvocations,
    BigDecimal estimatedSearchCostPerRequestUsd,
    BigDecimal maximumSearchCostUsd,
    String costCoverage,
    String modelCostCoverage,
    String pricingSource,
    LocalDate pricingObservedOn) {}
