package com.marketinghub.product.service.financialsummary;

import java.math.BigDecimal;

/** Responsabilidade: representar um valor financeiro em reais e dólares. */
public record ProductFinancialAmountResponse(BigDecimal brl, BigDecimal usd) {}
