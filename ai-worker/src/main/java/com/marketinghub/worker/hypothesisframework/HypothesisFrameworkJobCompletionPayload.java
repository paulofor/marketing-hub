package com.marketinghub.worker.hypothesisframework;

import java.math.BigDecimal;

public record HypothesisFrameworkJobCompletionPayload(
        String responseContent,
        String rawResponse,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
