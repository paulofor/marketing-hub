package com.marketinghub.financialagent.service.registerProviderCreditPurchase;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: expor uma recarga de créditos persistida no Financeiro. */
public record ProviderCreditPurchaseResponse(
    Long id,
    String provider,
    Instant purchasedAt,
    BigDecimal amount,
    String currency,
    Integer creditsPurchased,
    String evidenceReference,
    Instant createdAt) {}
