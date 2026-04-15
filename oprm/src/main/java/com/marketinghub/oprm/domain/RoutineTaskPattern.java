package com.marketinghub.oprm.domain;

import java.util.List;

public record RoutineTaskPattern(
        String taskLabel,
        String taskSummary,
        String triggerSummary,
        String frequencySignal,
        String timeCostSignal,
        String toolingSummary,
        List<String> evidenceRefs) {
}
