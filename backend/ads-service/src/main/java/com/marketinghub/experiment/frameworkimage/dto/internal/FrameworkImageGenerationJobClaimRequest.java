package com.marketinghub.experiment.frameworkimage.dto.internal;

import jakarta.validation.constraints.NotBlank;

public record FrameworkImageGenerationJobClaimRequest(@NotBlank String workerId) {
}
