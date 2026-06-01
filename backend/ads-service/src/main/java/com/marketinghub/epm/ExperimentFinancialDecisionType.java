package com.marketinghub.epm;

/**
 * Representa as decisões financeiras iniciais permitidas para experimentos e hipóteses.
 */
public enum ExperimentFinancialDecisionType {
    CONTINUE,
    ITERATE_CREATIVE,
    ITERATE_LANDING,
    ITERATE_OFFER,
    SCALE_CONTROLLED,
    KILL_EXPERIMENT,
    REFORMULATE_HYPOTHESIS,
    KILL_HYPOTHESIS,
    INCONCLUSIVE
}
