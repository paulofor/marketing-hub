package com.marketinghub.salesvideo.autonomy.v1;

import com.marketinghub.agenttask.AgentTaskExecutionAuditRequest;
import com.marketinghub.agenttask.AgentTaskModelUsageRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: concentrar os contratos da API de ciclos autônomos de vídeo v1. */
public final class VideoProductionCycleContracts {
  /** Impede instanciação do agrupador de contratos. */
  private VideoProductionCycleContracts() {}

  /** Solicita um ciclo sem autorizar consumo financeiro. */
  public record CreateRequest(
      @NotNull Long videoProjectId,
      @NotNull @DecimalMin(value = "0.01") BigDecimal budgetLimitUsd,
      String productionProfile,
      @NotBlank String learningObjective,
      @NotBlank String successCriterion,
      @NotBlank String requestedBy) {}

  /** Registra a decisão independente de Plutus. */
  public record FinancialDecisionRequest(
      @NotBlank String decision,
      @NotBlank String reason,
      @NotBlank String decidedByAgentKey,
      @NotBlank String recommendedAggregator,
      @NotBlank String recommendedRoute,
      @NotNull @PositiveOrZero BigDecimal estimatedCostUsd,
      @NotBlank String costBenefitBasis,
      @NotBlank String creditAction,
      @PositiveOrZero BigDecimal recommendedRechargeCredits,
      String rechargeUrl) {}

  /** Registra prompt, resposta bruta e consumo da análise de Plutus antes da decisão do gate. */
  public record FinancialReviewAuditRequest(
      @NotBlank @Size(max = 16_777_215) String rawModelResponse,
      @NotNull @Valid AgentTaskExecutionAuditRequest executionAudit,
      List<@Valid AgentTaskModelUsageRequest> modelUsages) {}

  /** Entrega a Plutus somente o contexto e a eventual resposta necessária para retomar o gate. */
  public record FinancialReviewPendingResponse(
      Long id,
      Long videoProjectId,
      Long productId,
      Long commercialPlanId,
      Long experimentId,
      String status,
      BigDecimal budgetLimitUsd,
      BigDecimal knownCostUsd,
      String financialSnapshot,
      Long agentTaskId,
      String financialReviewRawResponse) {}

  /** Expõe estado, custo e decisão persistidos do ciclo. */
  public record Response(
      Long id,
      Long videoProjectId,
      Long productId,
      Long commercialPlanId,
      Long experimentId,
      String status,
      BigDecimal budgetLimitUsd,
      BigDecimal knownCostUsd,
      String learningObjective,
      String successCriterion,
      VideoProviderPreflightContracts.SnapshotResponse providerPreflight,
      String financialSnapshot,
      String financialDecision,
      String financialReason,
      String recommendedAggregator,
      String recommendedRoute,
      BigDecimal estimatedCostUsd,
      String costBenefitBasis,
      String creditAction,
      BigDecimal recommendedRechargeCredits,
      String rechargeUrl,
      Long salesVideoJobId,
      Long lastFailedJobId,
      String lastApolloFailureCode,
      String lastApolloFailureDetail,
      Instant lastApolloFailureAt,
      Long monitoredTaskCount,
      Long monitoredCredits,
      String budgetMonitorStatus,
      String budgetAlertCode,
      String budgetAlertDetail,
      Instant budgetAlertAt,
      Integer providerClipDurationSeconds,
      Integer generationClipCount,
      Integer editCutCount,
      boolean textAppliedInPostProduction,
      Long agentTaskId,
      Instant createdAt,
      Instant updatedAt) {}
}
