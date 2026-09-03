package com.marketinghub.salesvideo.autonomy.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: concentrar os contratos v1 de preflight e reserva de provedores de vídeo. */
public final class VideoProviderPreflightContracts {
  /** Impede instanciação do agrupador de contratos. */
  private VideoProviderPreflightContracts() {}

  /** Entrega ao executor apenas o contexto necessário para consultar a conta e montar o dry run. */
  public record PendingResponse(
      Long preflightId,
      Long cycleId,
      String aggregatorName,
      String accountKey,
      String productionProfile,
      BigDecimal maxCredits,
      Integer targetDurationSeconds,
      Integer providerClipDurationSeconds,
      Integer generationClipCount,
      String aspectRatio,
      String resolution,
      boolean audio,
      String title,
      String objective,
      String hookText,
      String scriptText,
      String scenePlan,
      String characterBible,
      String environmentBible,
      String visualStyleGuide,
      String continuityRules,
      String learningObjective,
      String successCriterion) {}

  /** Recebe snapshot sanitizado e dry run sem permitir ao executor decidir ou reservar créditos. */
  public record ResultRequest(
      @NotBlank String status,
      @NotBlank String accountKey,
      String routerConfigId,
      String payloadSha256,
      String executionRequestsJson,
      String organizationSnapshotJson,
      String routingResponseJson,
      String selectedRoutesJson,
      @PositiveOrZero BigDecimal estimatedCredits,
      @PositiveOrZero BigDecimal officialBalanceCredits,
      @PositiveOrZero Long maxMonthlyCreditSpend,
      String quotaSnapshotJson,
      String usageSnapshotJson,
      String failureCode,
      String failureDetail,
      @NotBlank String sourceUrl,
      @NotNull Instant observedAt) {}

  /** Expõe o parecer técnico-financeiro persistido para Plutus e para o Estúdio. */
  public record SnapshotResponse(
      Long id,
      String status,
      String productionProfile,
      String aggregatorName,
      String accountKey,
      String routerConfigId,
      String payloadSha256,
      String selectedRoutesJson,
      BigDecimal estimatedCredits,
      BigDecimal estimatedCostUsd,
      BigDecimal maximumAuthorizedCredits,
      BigDecimal maximumAuthorizedCostUsd,
      BigDecimal officialBalanceCredits,
      BigDecimal reservedCreditsSnapshot,
      BigDecimal availableCreditsSnapshot,
      Long maxMonthlyCreditSpend,
      String quotaSnapshotJson,
      String failureCode,
      String failureDetail,
      String sourceUrl,
      String rechargeUrl,
      Instant observedAt,
      Instant expiresAt,
      ReservationResponse reservation) {}

  /** Expõe a reserva e o custo realizado sem revelar credenciais do agregador. */
  public record ReservationResponse(
      Long id,
      String status,
      BigDecimal reservedCredits,
      BigDecimal reservedCostUsd,
      BigDecimal actualCredits,
      BigDecimal actualCostUsd,
      Instant expiresAt,
      Instant reservedAt,
      Instant settledAt,
      Instant releasedAt) {}
}
