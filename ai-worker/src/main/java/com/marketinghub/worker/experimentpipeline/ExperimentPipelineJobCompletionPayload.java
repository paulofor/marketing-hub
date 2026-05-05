package com.marketinghub.worker.experimentpipeline;

import java.math.BigDecimal;

public record ExperimentPipelineJobCompletionPayload(
        String responseContent,
        String rawResponse,
        String requestBodyJson,
        String openAiJobId,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
