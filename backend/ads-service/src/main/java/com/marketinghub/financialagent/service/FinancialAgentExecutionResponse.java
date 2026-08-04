package com.marketinghub.financialagent.service;

import com.marketinghub.financialagent.FinancialAgentExecutionStatus;
import java.time.Instant;

/** Responsabilidade: expor uma execucao financeira sem revelar entidades internas. */
public record FinancialAgentExecutionResponse(
    Long id,
    Long commercialPlanId,
    FinancialAgentExecutionStatus status,
    String authorityMode,
    String financialSnapshot,
    String reconciliationJson,
    String dailyReport,
    String model,
    String errorMessage,
    Instant startedAt,
    Instant finishedAt,
    Instant createdAt) {}
