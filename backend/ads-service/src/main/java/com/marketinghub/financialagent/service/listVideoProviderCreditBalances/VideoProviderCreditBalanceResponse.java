package com.marketinghub.financialagent.service.listVideoProviderCreditBalances;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: expor saldo estimado e capacidade operacional de um provedor de vídeo. */
public record VideoProviderCreditBalanceResponse(
    String provider,
    String status,
    String balanceNature,
    long purchasedCredits,
    Long estimatedConsumedCredits,
    Long estimatedAvailableCredits,
    String referenceModel,
    Integer referenceClipSeconds,
    Integer referenceClipCredits,
    Long estimatedReferenceClips,
    Instant lastPurchaseAt,
    Instant lastCreditFailureAt,
    Long lastCreditFailureJobId,
    String lastCreditFailureDetail,
    BigDecimal knownConsumedCostUsd,
    long unknownCostAttempts,
    long acceptedSceneRequests,
    List<VideoProviderSceneRequestResponse> sceneRequests,
    String creditsUrl,
    String aggregatorName,
    String accountKey,
    String officialSnapshotStatus,
    BigDecimal officialBalanceCredits,
    BigDecimal reservedCredits,
    BigDecimal officialAvailableCredits,
    Long maxMonthlyCreditSpend,
    String quotaSnapshotJson,
    Instant officialObservedAt,
    Instant officialExpiresAt,
    String officialSourceUrl) {}
