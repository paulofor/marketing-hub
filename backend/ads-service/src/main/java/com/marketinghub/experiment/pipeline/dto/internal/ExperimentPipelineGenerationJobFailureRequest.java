package com.marketinghub.experiment.pipeline.dto.internal;

import jakarta.validation.constraints.NotBlank;

public record ExperimentPipelineGenerationJobFailureRequest(@NotBlank String errorMessage) {
}
