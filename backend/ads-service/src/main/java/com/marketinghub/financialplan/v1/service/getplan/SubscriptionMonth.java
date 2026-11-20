package com.marketinghub.financialplan.v1.service.getplan;

import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import java.math.BigDecimal;

/** Responsabilidade: mostrar o cenário-base de um mês mesmo antes de completar os custos. */
public record SubscriptionMonth(
    Integer newSubscribers,
    Integer renewals,
    Integer freeUsers,
    Integer payingSubscribers,
    BigDecimal recurringRevenueBrl,
    BigDecimal freeTierCostBrl) {

  /** Calcula somente parcelas que possuem fonte, sem presumir conversão ou renovação futura. */
  public static SubscriptionMonth project(PlanAssumptions assumptions) {
    var terms = assumptions.subscription();
    if (terms == null) return null;
    Integer additions =
        assumptions.scenarios().stream()
            .filter(s -> s.code() == PlanAssumptions.ScenarioCode.BASE)
            .findFirst()
            .map(PlanAssumptions.Scenario::customers)
            .orElse(null);
    Integer paying =
        additions == null || terms.renewals() == null ? null : additions + terms.renewals();
    BigDecimal revenue =
        paying == null || assumptions.priceBrl() == null
            ? null
            : assumptions.priceBrl().multiply(BigDecimal.valueOf(paying));
    return new SubscriptionMonth(
        additions, terms.renewals(), terms.freeUsers(), paying, revenue, terms.freeTierCost());
  }
}
