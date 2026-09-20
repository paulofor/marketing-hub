package com.marketinghub.experiment.funnel;

import java.math.BigDecimal;

/** Centraliza os limites financeiros canônicos usados para proteger experimentos em mídia paga. */
public final class ExperimentFinancialGuardrailPolicy {

  private static final BigDecimal ZERO_PRIMARY_RESULT_MINIMUM_SPEND = new BigDecimal("25.00");

  /** Impede instanciação de uma política formada apenas por valores canônicos. */
  private ExperimentFinancialGuardrailPolicy() {}

  /** Informa o gasto que aciona a parada automática quando ainda não há resultado primário. */
  public static BigDecimal zeroPrimaryResultMinimumSpend() {
    return ZERO_PRIMARY_RESULT_MINIMUM_SPEND;
  }
}
