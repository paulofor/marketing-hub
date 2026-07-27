package com.marketinghub.product.service.financialsummary;

import java.time.Instant;

/** Responsabilidade: expor o resultado financeiro consolidado de um mês do produto. */
public record ProductFinancialMonthlyResultResponse(
    Instant monthStart,
    String monthLabel,
    ProductFinancialAmountResponse cost,
    ProductFinancialAmountResponse revenue,
    ProductFinancialAmountResponse profit) {}
