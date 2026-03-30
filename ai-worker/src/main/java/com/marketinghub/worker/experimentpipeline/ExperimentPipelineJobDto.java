package com.marketinghub.worker.experimentpipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExperimentPipelineJobDto(
        UUID id,
        Long experimentId,
        String section,
        String model,
        String prompt,
        String requestBodyJson,
        Instant createdAt) {
}
