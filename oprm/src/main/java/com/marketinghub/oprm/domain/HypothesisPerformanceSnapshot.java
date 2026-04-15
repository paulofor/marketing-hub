package com.marketinghub.oprm.domain;

public record HypothesisPerformanceSnapshot(
        String hypothesisId,
        String hypothesisLabel,
        double ctr,
        double conversionRate,
        double cpa,
        double confidenceScore) {
}
