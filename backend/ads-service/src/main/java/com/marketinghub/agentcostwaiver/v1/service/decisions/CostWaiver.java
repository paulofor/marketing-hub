package com.marketinghub.agentcostwaiver.v1.service.decisions;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Contratos da dispensa de conciliação, sem atribuir valor aos custos desconhecidos. */
public final class CostWaiver {
  /** Impede instanciar o agrupador de contratos. */
  private CostWaiver() {}

  /** Decisão explícita com seleção fixa e proteção contra repetição e contexto desatualizado. */
  public record Request(
      @NotNull UUID requestKey,
      @NotNull Long chainId,
      @NotNull Long expectedRevision,
      @NotEmpty @Size(max = 500) List<@NotNull Long> taskIds,
      @NotBlank @Size(max = 160) String operatorName,
      @NotBlank @Size(max = 2000) String reason,
      @AssertTrue boolean confirmed) {}

  /** Tarefa auditável e elegibilidade calculada no backend. */
  public record Task(
      Long taskId,
      Long experimentId,
      String status,
      BigDecimal costUsd,
      boolean waived,
      boolean selectable) {}

  /** Recibo imutável, inclusive após recuperação do custo. */
  public record Receipt(
      Long eventId,
      Long cycleId,
      String operatorName,
      Instant decidedAt,
      String reason,
      List<Long> taskIds) {}

  /** Fotografia atual que separa dispensa de custo conciliado. */
  public record View(List<Task> tasks, List<Receipt> history, long blockingTasks) {}
}
