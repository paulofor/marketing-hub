package com.marketinghub.experiment.pipeline.dto.internal;

import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ExperimentPipelineGenerationJobDto(
        UUID id,
        Long experimentId,
        ExperimentPipelineSection section,
        String status,
        String stage,
        String customInstructions,
        String errorMessage,
        String model,
        String prompt,
        String requestBodyJson,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt) {
}
