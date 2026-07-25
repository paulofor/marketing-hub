package com.marketinghub.product.service.financialsummary;

/** Responsabilidade: expor uma linha financeira categorizada do produto. */
public record ProductFinancialLineResponse(
    String type,
    String label,
    ProductFinancialAmountResponse monthly,
    ProductFinancialAmountResponse annual,
    String source) {}
