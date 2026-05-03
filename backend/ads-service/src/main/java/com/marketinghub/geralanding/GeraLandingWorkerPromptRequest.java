package com.marketinghub.geralanding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GeraLandingWorkerPromptRequest(
        @NotNull Long experimentId,
        @NotBlank String stageCode,
        @NotBlank String executionId,
        @NotBlank String promptContent) {
}
