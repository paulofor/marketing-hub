package com.marketinghub.experiment.pipeline.dto.internal;

import com.marketinghub.experiment.pipeline.ExperimentPipelineGenerationJobStage;
import jakarta.validation.constraints.NotNull;

public record ExperimentPipelineGenerationJobStageUpdateRequest(
        @NotNull ExperimentPipelineGenerationJobStage stage) {
}
