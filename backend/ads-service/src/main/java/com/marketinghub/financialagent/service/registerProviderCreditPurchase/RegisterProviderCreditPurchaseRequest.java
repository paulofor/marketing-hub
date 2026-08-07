package com.marketinghub.financialagent.service.registerProviderCreditPurchase;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: receber os dados auditáveis de uma recarga de créditos. */
public record RegisterProviderCreditPurchaseRequest(
    @NotNull Instant purchasedAt,
    @NotNull @DecimalMin(value = "0.000001") BigDecimal amount,
    @NotNull @Size(min = 3, max = 3) String currency,
    @NotNull @Positive Integer creditsPurchased,
    @Size(max = 500) String evidenceReference) {}
