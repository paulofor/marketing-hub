package com.marketinghub.oprm.domain;

import java.time.Instant;

public record OccupationFeedbackHistoryEntry(
        Instant generatedAt,
        double previousRoutineConfidence,
        double recalibratedRoutineConfidence,
        double previousFrameworkConfidence,
        double recalibratedFrameworkConfidence,
        double averageHypothesisImpact,
        String notes) {
}
