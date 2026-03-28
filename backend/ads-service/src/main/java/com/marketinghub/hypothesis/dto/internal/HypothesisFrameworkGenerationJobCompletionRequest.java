package com.marketinghub.hypothesis.dto.internal;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record HypothesisFrameworkGenerationJobCompletionRequest(
        @NotBlank String responseContent,
        String rawResponse,
        String requestBodyJson,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
