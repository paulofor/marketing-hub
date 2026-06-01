package com.marketinghub.epm.service.listProductPriceScenarios;

import java.time.Instant;

/** Resposta com um cenário de preço e ponto de equilíbrio do EPM. */
public record ProductPriceScenarioResponse(Long id, Long financialPlanId, String name, Long priceCents, Long expectedPaymentFeeCents, Long expectedPlatformFeeCents, Long expectedTaxCents, Long expectedNetRevenuePerSaleCents, Long totalBudgetCents, Integer breakEvenSales, String notes, Instant createdAt, Instant updatedAt) {}
