package com.marketinghub.experiment.funnel;

import java.math.BigDecimal;

/** Centraliza os limites financeiros canônicos usados para proteger experimentos em mídia paga. */
public final class ExperimentFinancialGuardrailPolicy {

  private static final BigDecimal ZERO_PRIMARY_RESULT_MINIMUM_SPEND = new BigDecimal("25.00");

  /** Impede instanciação de uma política formada apenas por valores canônicos. */
  private ExperimentFinancialGuardrailPolicy() {}

  /** Resolve exceção individual, sempre limitada ao teto absoluto autorizado. */
  public static BigDecimal zeroPrimaryResultMinimumSpend(
      com.marketinghub.experiment.Experiment experiment) {
    if (experiment == null
        || experiment.getZeroResultSpendLimit() == null
        || experiment.getMediaSpendLimit() == null
        || experiment.getZeroResultSpendLimit().signum() <= 0) {
      return ZERO_PRIMARY_RESULT_MINIMUM_SPEND;
    }
    return experiment.getZeroResultSpendLimit().min(experiment.getMediaSpendLimit());
  }

  /** Informa o gasto que aciona a parada automática quando ainda não há resultado primário. */
  public static BigDecimal zeroPrimaryResultMinimumSpend() {
    return ZERO_PRIMARY_RESULT_MINIMUM_SPEND;
  }
}
