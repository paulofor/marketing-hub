package com.marketinghub.hypothesis.dto.internal;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record HypothesisFrameworkGenerationJobCompletionRequest(
        @NotBlank String responseContent,
        String rawResponse,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
