package com.marketinghub.experiment.pipeline.dto;

import com.marketinghub.experiment.pipeline.ExperimentPipelineSection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ExperimentPipelineGenerationJobDetailDto(
        UUID id,
        Long experimentId,
        ExperimentPipelineSection section,
        String status,
        String stage,
        String model,
        String workerId,
        String customInstructions,
        String prompt,
        String requestBodyJson,
        String responseContent,
        String rawResponse,
        String errorMessage,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt) {
}
