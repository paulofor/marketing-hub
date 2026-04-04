package com.marketinghub.experiment.pipeline.dto;

import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ExperimentPipelineGenerationJobSummaryDto(
        UUID id,
        Long experimentId,
        ExperimentPipelineSection section,
        String status,
        String stage,
        String model,
        String errorMessage,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt) {
}
