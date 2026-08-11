package com.marketinghub.salesvideo.autonomy.v1;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: concentrar os contratos da API de ciclos autônomos de vídeo v1. */
public final class VideoProductionCycleContracts {
  /** Impede instanciação do agrupador de contratos. */
  private VideoProductionCycleContracts() {}

  /** Solicita um ciclo sem autorizar consumo financeiro. */
  public record CreateRequest(
      @NotNull Long videoProjectId,
      @NotNull @DecimalMin(value = "0.01") BigDecimal budgetLimitUsd,
      @NotBlank String requestedBy) {}

  /** Registra a decisão independente de Plutus. */
  public record FinancialDecisionRequest(
      @NotBlank String decision, @NotBlank String reason, @NotBlank String decidedByAgentKey) {}

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
      String financialSnapshot,
      String financialDecision,
      String financialReason,
      Long salesVideoJobId,
      Long agentTaskId,
      Instant createdAt,
      Instant updatedAt) {}
}
