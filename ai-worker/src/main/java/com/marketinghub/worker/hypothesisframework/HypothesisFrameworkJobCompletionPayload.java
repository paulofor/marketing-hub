package com.marketinghub.worker.hypothesisframework;

import java.math.BigDecimal;

public record HypothesisFrameworkJobCompletionPayload(
        String responseContent,
        String rawResponse,
        String requestBodyJson,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
