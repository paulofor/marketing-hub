package com.marketinghub.oprm.domain;

import java.util.List;

public record DesiredOutcomeSignal(
        String outcomeLabel,
        String outcomeSummary,
        List<String> linkedPainRefs,
        double impactScore,
        List<String> evidenceRefs) {
}
