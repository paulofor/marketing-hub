package com.marketinghub.financialagentworker;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: representar um modelo cujo preço precisa ser pesquisado por Plutus. */
public record ProviderPricingCandidate(
    Long id,
    String displayName,
    String manufacturerName,
    String aggregatorName,
    String providerAccountKey,
    String routeKey,
    String providerName,
    String externalModelId,
    String documentationUrl,
    BigDecimal pricingAmountUsd,
    Instant pricingObservedAt,
    String pricingResearchStatus,
    boolean pricingStale) {}
