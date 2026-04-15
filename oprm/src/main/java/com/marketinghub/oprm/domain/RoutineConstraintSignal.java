package com.marketinghub.oprm.domain;

import java.util.List;

public record RoutineConstraintSignal(
        String constraintType,
        String constraintSummary,
        double severityScore,
        String contextSummary,
        List<String> evidenceRefs) {
}
