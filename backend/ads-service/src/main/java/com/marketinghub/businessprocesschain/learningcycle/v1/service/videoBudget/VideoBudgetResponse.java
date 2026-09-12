package com.marketinghub.businessprocesschain.learningcycle.v1.service.videoBudget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: apresentar contexto, escopo, autorização e histórico financeiro dos vídeos. */
public record VideoBudgetResponse(
    Long productId,
    String productName,
    String internalName,
    Long chainDefinitionId,
    Long processDefinitionId,
    Long cycleId,
    Long experimentId,
    String productVersion,
    long revision,
    String stageLabel,
    String status,
    String instruction,
    String scope,
    String currency,
    boolean canAuthorize,
    String blocker,
    Authorization currentAuthorization,
    List<Authorization> history,
    String cycleUrl,
    String financeUrl) {
  /** Preserva o recibo e sua vigência sem representar parecer de Plutus ou débito confirmado. */
  public record Authorization(
      Long eventId,
      String reference,
      BigDecimal budgetLimitUsd,
      String operatorName,
      String justification,
      Instant createdAt,
      String productVersion,
      boolean current) {}
}
