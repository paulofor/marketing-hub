package com.marketinghub.experiment.pipeline.dto;

import java.util.List;

public record PipelineOperationalMetricsDto(
        String generatedAt,
        boolean lhmRegistryEnabled,
        boolean lhmAuditGateEnabled,
        int lookbackJobs,
        double averageDurationSeconds,
        double overallFailureRate,
        double reworkRate,
        double placeholderRate,
        double averageQualityScore,
        List<PipelineSectionMetricDto> sections) {
}

