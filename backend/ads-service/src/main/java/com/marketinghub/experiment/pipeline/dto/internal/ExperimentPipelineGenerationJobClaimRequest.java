package com.marketinghub.experiment.pipeline.dto.internal;

import jakarta.validation.constraints.NotBlank;

public record ExperimentPipelineGenerationJobClaimRequest(@NotBlank String workerId) {
}
