package com.marketinghub.oprm.domain;

import java.util.List;

public record RoutinePainSignal(
        String painLabel,
        String painSummary,
        String painType,
        double painIntensityScore,
        double painRecurrenceScore,
        String workaroundSummary,
        List<String> evidenceRefs) {
}
