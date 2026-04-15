package com.marketinghub.oprm.domain;

import java.util.List;

public record HypothesisRoutineFit(
        String hypothesisId,
        String hypothesisLabel,
        double performanceScore,
        double routineFitScore,
        double weightedImpactScore,
        List<String> linkedRoutineSignals,
        String comparisonSummary) {
}
