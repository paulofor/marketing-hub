package com.marketinghub.oprm.domain;

import java.util.List;

public record MechanismOpportunitySignal(
        String mechanismLabel,
        String mechanismSummary,
        List<String> linkedTaskRefs,
        List<String> linkedPainRefs,
        double commercialFitScore,
        double implementationEffortScore,
        List<String> evidenceRefs) {
}
