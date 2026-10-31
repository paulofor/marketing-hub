package com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview;

import com.marketinghub.financialplan.v1.service.getplan.PlanView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: apresentar demonstração financeira segregada e recibos do ciclo. */
public record CycleFinancialReview(
    Long productId,
    String productName,
    Long chainId,
    Long cycleId,
    Long experimentId,
    String productVersion,
    long revision,
    Instant observedAt,
    boolean canAuthorize,
    String authorizationBlocker,
    String cycleUrl,
    String mediaUrl,
    String videoUrl,
    String planUrl,
    String operatorNotesUrl,
    String guidance,
    BigDecimal mediaBudgetBrl,
    BigDecimal videoBudgetUsd,
    BigDecimal knownPreparationUsd,
    int unreportedTasks,
    String costCoverage,
    BigDecimal availableUsd,
    List<CostSlice> costs,
    Authorization authorization,
    List<Authorization> history,
    PlanView financialPlan,
    FinancialAdvice advice) {
  /** Responsabilidade: expor custos agregados por experimento sem carregar auditorias. */
  public record CostSlice(Long experimentId, BigDecimal knownUsd, int unreportedTasks) {}

  /** Responsabilidade: preservar a decisão, as premissas e o contexto exato do aceite humano. */
  public record Authorization(
      Long eventId,
      String reference,
      BigDecimal totalLimitUsd,
      BigDecimal reserveUsd,
      String operatorName,
      String justification,
      Instant approvedAt,
      String productVersion,
      boolean current) {}
}
