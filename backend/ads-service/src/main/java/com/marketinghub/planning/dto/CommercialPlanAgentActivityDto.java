package com.marketinghub.planning.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: expor a atuação auditável dos agentes vinculada a um plano comercial. */
public record CommercialPlanAgentActivityDto(
    Long commercialPlanId,
    Integer currentVersion,
    BigDecimal budgetLimitBrl,
    BigDecimal campaignCostBrl,
    BigDecimal aiCostBrl,
    BigDecimal totalCostBrl,
    BigDecimal revenueBrl,
    BigDecimal videoBudgetLimitUsd,
    BigDecimal videoKnownCostUsd,
    long openTasks,
    long pendingDecisions,
    List<Entry> entries) {

  /** Responsabilidade: representar um registro operacional ou financeiro de um agente. */
  public record Entry(
      String recordType,
      String agentKey,
      String agentNickname,
      String title,
      String status,
      String detail,
      String finalOpinion,
      String difficulty,
      boolean externalDecisionRequired,
      String externalDecision,
      String sourceReference,
      BigDecimal budgetLimitUsd,
      BigDecimal knownCostUsd,
      String financialDecision,
      Instant occurredAt) {}
}
