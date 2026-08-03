package com.marketinghub.growthoperator.service.result;

import com.marketinghub.growthoperator.GrowthOperatorDecision;
import java.math.BigDecimal;

/** Responsabilidade: receber o diagnostico estruturado produzido pelo worker em sandbox. */
public record CompleteGrowthOperatorRequest(
    String alternativesJson,
    String diagnosisJson,
    String rawModelResponse,
    GrowthOperatorDecision recommendedDecision,
    String recommendedAction,
    String model,
    Long inputTokens,
    Long outputTokens,
    BigDecimal estimatedCost) {}
