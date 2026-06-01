package com.marketinghub.epm.service.getFinancialPlanSummary;

/** Resposta consolidada com métricas financeiras operacionais de um plano EPM. */
public record FinancialPlanSummaryResponse(Long planId, Long totalBudgetCents, Long plannedExperimentBudgetCents, Long actualSpendCents, Long revenueCents, Long grossProfitCents, Long estimatedNetProfitCents, Integer niches, Integer hypotheses, Integer experiments, Integer experimentsWithPurchase, Integer experimentsWithoutSignal) {}
