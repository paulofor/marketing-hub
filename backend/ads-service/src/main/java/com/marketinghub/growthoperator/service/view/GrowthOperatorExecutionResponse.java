package com.marketinghub.growthoperator.service.view;

import com.marketinghub.growthoperator.GrowthOperatorDecision;
import com.marketinghub.growthoperator.GrowthOperatorExecutionStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: expor uma execucao do Operador de Crescimento para tela ou worker. */
public record GrowthOperatorExecutionResponse(
    Long id,
    Long commercialPlanId,
    Integer weekNumber,
    GrowthOperatorExecutionStatus status,
    String authorityMode,
    String objective,
    String blocker,
    String evidenceSnapshot,
    String alternativesJson,
    String diagnosisJson,
    GrowthOperatorDecision recommendedDecision,
    String recommendedAction,
    String errorMessage,
    String model,
    Long inputTokens,
    Long outputTokens,
    BigDecimal estimatedCost,
    Instant startedAt,
    Instant finishedAt,
    Instant createdAt) {}
