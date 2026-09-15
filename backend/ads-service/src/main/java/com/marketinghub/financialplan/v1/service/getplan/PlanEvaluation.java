package com.marketinghub.financialplan.v1.service.getplan;

import java.math.BigDecimal;
import java.util.List;

/** Responsabilidade: expor projeções calculadas e causas que impedem recomendar viabilidade. */
public record PlanEvaluation(
    String status, String label, List<String> blockers, List<ScenarioResult> scenarios) {
  /** Resultado econômico projetado; campos incalculáveis permanecem nulos. */
  public record ScenarioResult(
      String code,
      String label,
      Integer customers,
      int attemptsPerCustomer,
      BigDecimal grossRevenueBrl,
      BigDecimal netRevenueBrl,
      BigDecimal aiCostPerCustomerBrl,
      BigDecimal aiCostPerUsefulUnitBrl,
      BigDecimal variableDeliveryPerCustomerBrl,
      BigDecimal contributionBeforeCacBrl,
      BigDecimal contributionAfterCacBrl,
      BigDecimal marginPercent,
      BigDecimal maximumAffordableCacBrl,
      BigDecimal operatingResultBrl,
      BigDecimal resultAfterInvestmentBrl,
      BigDecimal breakEvenCustomers,
      BigDecimal aiToNetRevenuePercent,
      boolean viable,
      List<String> blockers) {}
}
