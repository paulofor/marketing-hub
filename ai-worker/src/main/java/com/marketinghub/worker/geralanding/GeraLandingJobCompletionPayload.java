package com.marketinghub.worker.geralanding;

import java.math.BigDecimal;

public record GeraLandingJobCompletionPayload(
        String responseContent,
        String rawResponse,
        String requestBodyJson,
        String openAiJobId,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
