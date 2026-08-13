package com.marketinghub.financialagent.service.listVideoProviderCreditBalances;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: expor uma cena aceita e cobrável pelo provedor de vídeo. */
public record VideoProviderSceneRequestResponse(
    Long jobId,
    Long productionCycleId,
    int sceneNumber,
    int plannedSceneCount,
    String providerTaskId,
    String model,
    Integer durationSeconds,
    Integer estimatedCredits,
    BigDecimal estimatedCostUsd,
    Integer billedCredits,
    BigDecimal billedCostUsd,
    String settlementStatus,
    String billingEvidence,
    Instant acceptedAt) {}
