package com.marketinghub.salesvideo.service.providerpreflight;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: definir os dados internos do preflight financeiro de provedores de vídeo. */
public final class VideoProviderFinancialPreflightData {
  /** Impede instanciação do agrupador de dados internos. */
  private VideoProviderFinancialPreflightData() {}

  /** Entrega ao executor o contexto mínimo para consultar a conta e montar o dry run. */
  public record Pending(
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

  /** Representa o snapshot sanitizado e o resultado do dry run recebido do executor. */
  public record Result(
      String status,
      String accountKey,
      String routerConfigId,
      String payloadSha256,
      String executionRequestsJson,
      String organizationSnapshotJson,
      String routingResponseJson,
      String selectedRoutesJson,
      BigDecimal estimatedCredits,
      BigDecimal officialBalanceCredits,
      Long maxMonthlyCreditSpend,
      String quotaSnapshotJson,
      String usageSnapshotJson,
      String failureCode,
      String failureDetail,
      String sourceUrl,
      Instant observedAt) {}

  /** Representa somente os campos financeiros necessários para validar o parecer de Plutus. */
  public record FinancialDecision(
      String recommendedAggregator,
      String recommendedRoute,
      BigDecimal estimatedCostUsd,
      String creditAction,
      BigDecimal recommendedRechargeCredits,
      String rechargeUrl) {}

  /** Expõe o preflight e sua reserva para a fachada que monta o contrato HTTP. */
  public record Snapshot(
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
      Reservation reservation) {}

  /** Expõe a reserva e o custo realizado sem revelar credenciais do agregador. */
  public record Reservation(
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
