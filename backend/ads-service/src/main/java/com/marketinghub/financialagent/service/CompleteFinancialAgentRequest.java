package com.marketinghub.financialagent.service;

import java.math.BigDecimal;

/** Responsabilidade: receber o resultado estruturado produzido pelo worker financeiro. */
public record CompleteFinancialAgentRequest(
    String reconciliationJson,
    String dailyReport,
    String rawModelResponse,
    String model,
    BigDecimal estimatedCost,
    String promptSent,
    String agentPromptPart,
    String activityPromptPart,
    String reasoningEffort,
    String requestedServiceTier,
    String effectiveServiceTier,
    String serviceTierExceptionReason,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens) {}
