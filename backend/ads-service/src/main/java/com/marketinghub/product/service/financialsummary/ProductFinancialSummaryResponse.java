package com.marketinghub.product.service.financialsummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: expor o resumo financeiro consolidado de um produto comercial. */
public record ProductFinancialSummaryResponse(
    Long productId,
    String productName,
    String productSlug,
    BigDecimal exchangeRateBrlPerUsd,
    Instant monthStart,
    Instant yearStart,
    List<ProductFinancialLineResponse> costs,
    ProductFinancialLineResponse revenue,
    ProductFinancialLineResponse profit) {}
