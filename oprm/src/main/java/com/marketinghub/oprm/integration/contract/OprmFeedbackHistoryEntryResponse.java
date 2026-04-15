package com.marketinghub.oprm.integration.contract;

public record OprmFeedbackHistoryEntryResponse(
        String generatedAt,
        double previousRoutineConfidence,
        double recalibratedRoutineConfidence,
        double previousFrameworkConfidence,
        double recalibratedFrameworkConfidence,
        double averageHypothesisImpact,
        String notes
) {
}
