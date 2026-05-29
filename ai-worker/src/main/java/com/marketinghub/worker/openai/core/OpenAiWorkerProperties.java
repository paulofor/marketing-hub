package com.marketinghub.worker.openai.core;

import java.time.Duration;
import java.util.Objects;

public record OpenAiWorkerProperties(
        boolean enabled,
        int pendingLimit,
        Duration timeout
) {
    public OpenAiWorkerProperties {
        if (pendingLimit < 1) {
            throw new IllegalArgumentException("pendingLimit must be greater than zero");
        }
        Objects.requireNonNull(timeout, "timeout must not be null");
    }

    public static OpenAiWorkerProperties defaults() {
        return new OpenAiWorkerProperties(true, 10, Duration.ofMinutes(30));
    }
}
