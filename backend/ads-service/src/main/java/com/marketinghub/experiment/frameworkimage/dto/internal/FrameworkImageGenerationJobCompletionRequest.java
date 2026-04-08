package com.marketinghub.experiment.frameworkimage.dto.internal;

import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStage;

public record FrameworkImageGenerationJobCompletionRequest(
        FrameworkImageGenerationJobStage stage,
        String model,
        String prompt,
        String batchId,
        Long assetId,
        String sourceUrl,
        String webUrl) {
}
