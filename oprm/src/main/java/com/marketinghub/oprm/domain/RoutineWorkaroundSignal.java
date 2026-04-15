package com.marketinghub.oprm.domain;

import java.util.List;

public record RoutineWorkaroundSignal(
        String workaroundLabel,
        String workaroundSummary,
        String relatedTask,
        String relatedPain,
        double inefficiencyScore,
        List<String> evidenceRefs) {
}
