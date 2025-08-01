package com.marketinghub.experiment.service;

import java.math.BigDecimal;

/** Fetches metrics for an experiment. */
public interface ExperimentInsightsFetcher {
    Stats fetch(Long experimentId);

    record Stats(int clicks, int leads, BigDecimal cost) {
        public BigDecimal getCpl() {
            if (leads == 0) return BigDecimal.ZERO;
            return cost.divide(new BigDecimal(leads), java.math.MathContext.DECIMAL32);
        }
    }
}
