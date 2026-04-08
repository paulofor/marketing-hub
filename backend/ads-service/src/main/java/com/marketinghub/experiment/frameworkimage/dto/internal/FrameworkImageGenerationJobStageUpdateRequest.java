package com.marketinghub.experiment.frameworkimage.dto.internal;

import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStage;
import jakarta.validation.constraints.NotNull;

public record FrameworkImageGenerationJobStageUpdateRequest(@NotNull FrameworkImageGenerationJobStage stage) {
}
