package com.marketinghub.hypothesis.dto.internal;

import jakarta.validation.constraints.NotBlank;

public record HypothesisFrameworkGenerationJobClaimRequest(@NotBlank String workerId) {
}
