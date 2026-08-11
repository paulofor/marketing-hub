package com.marketinghub.financialagentworker;

import java.math.BigDecimal;

/** Responsabilidade: representar um ciclo de vídeo aguardando o gate de Plutus. */
public record VideoProductionCycleReview(
    Long id,
    Long videoProjectId,
    Long productId,
    Long commercialPlanId,
    Long experimentId,
    String status,
    BigDecimal budgetLimitUsd,
    BigDecimal knownCostUsd,
    String financialSnapshot) {}
