package com.marketinghub.epm.service.createProductPriceScenario;

import jakarta.validation.constraints.*;

/** Dados para criar um cenário de preço e ponto de equilíbrio do EPM. */
public record CreateProductPriceScenarioRequest(@NotBlank String name, @NotNull @Positive Long priceCents, Long expectedPaymentFeeCents, Long expectedPlatformFeeCents, Long expectedTaxCents, Long totalBudgetCents, String notes) {}
