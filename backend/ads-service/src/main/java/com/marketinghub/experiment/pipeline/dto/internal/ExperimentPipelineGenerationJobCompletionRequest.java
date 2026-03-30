package com.marketinghub.experiment.pipeline.dto.internal;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record ExperimentPipelineGenerationJobCompletionRequest(
        @NotBlank String responseContent,
        String rawResponse,
        String requestBodyJson,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
