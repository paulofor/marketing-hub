package com.marketinghub.worker.openai.core.model;

import java.time.Instant;
import java.util.Objects;

public record OpenAiDispatch(
        String openAiJobId,
        String prompt,
        String requestBodyJson,
        Instant dispatchedAt
) {
    public OpenAiDispatch {
        Objects.requireNonNull(prompt, "prompt must not be null");
        Objects.requireNonNull(requestBodyJson, "requestBodyJson must not be null");
        dispatchedAt = dispatchedAt == null ? Instant.now() : dispatchedAt;
    }
}
