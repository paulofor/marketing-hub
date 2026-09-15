package com.marketinghub.financialplan.v1.service.getplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: apresentar a revisão exata e o parecer auditável sem expor resposta bruta. */
public record PlanView(
    Long id,
    String scope,
    Long scopeId,
    Environment environment,
    String name,
    int revision,
    Long templateId,
    Long commercialPlanId,
    Integer commercialPlanVersion,
    String createdBy,
    Instant createdAt,
    PlanAssumptions assumptions,
    PlanEvaluation evaluation,
    boolean stale,
    List<String> pendingActions,
    boolean canRequestAnalysis,
    Analysis analysis) {
  /** Parecer de Plutus e cobertura do custo; não constitui autorização comercial. */
  public record Analysis(
      Long executionId,
      String status,
      String report,
      JsonNode result,
      String error,
      String model,
      BigDecimal costUsd,
      String costCoverage,
      Instant finishedAt) {}
}
