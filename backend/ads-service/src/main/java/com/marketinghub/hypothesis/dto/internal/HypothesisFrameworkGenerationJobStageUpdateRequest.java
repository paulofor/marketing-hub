package com.marketinghub.hypothesis.dto.internal;

import com.marketinghub.hypothesis.HypothesisFrameworkGenerationJobStage;
import jakarta.validation.constraints.NotNull;

public record HypothesisFrameworkGenerationJobStageUpdateRequest(
        @NotNull HypothesisFrameworkGenerationJobStage stage) {
}
