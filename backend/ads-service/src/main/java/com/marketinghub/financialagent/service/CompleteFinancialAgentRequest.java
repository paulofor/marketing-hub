package com.marketinghub.financialagent.service;

import java.math.BigDecimal;

/** Responsabilidade: receber o resultado estruturado produzido pelo worker financeiro. */
public record CompleteFinancialAgentRequest(
    String reconciliationJson,
    String dailyReport,
    String rawModelResponse,
    String model,
    BigDecimal estimatedCost) {}
