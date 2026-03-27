package com.marketinghub.hypothesis.dto.internal;

import jakarta.validation.constraints.NotBlank;

public record HypothesisFrameworkGenerationJobFailureRequest(@NotBlank String errorMessage) {
}
