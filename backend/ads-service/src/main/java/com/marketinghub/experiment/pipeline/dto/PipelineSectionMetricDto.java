package com.marketinghub.experiment.pipeline.dto;

public record PipelineSectionMetricDto(
        String section,
        long total,
        long failed,
        double failureRate,
        long rework,
        double reworkRate,
        long placeholders,
        double placeholderRate,
        double averageDurationSeconds,
        double averageQualityScore) {
}

