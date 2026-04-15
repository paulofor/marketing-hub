package com.marketinghub.oprm.dto;

public record OprmFeedbackHistoryEntryDto(
        String generatedAt,
        double previousRoutineConfidence,
        double recalibratedRoutineConfidence,
        double previousFrameworkConfidence,
        double recalibratedFrameworkConfidence,
        double averageHypothesisImpact,
        String notes
) {
}
