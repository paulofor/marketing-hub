package com.marketinghub.worker.openai.core.model;

import java.time.Instant;
import java.util.Objects;

public record StageExecution<I>(
        String idJob,
        Long aggregateId,
        String stageCode,
        String status,
        Instant executionRequestedAt,
        I input
) {
    public StageExecution {
        Objects.requireNonNull(idJob, "idJob must not be null");
        Objects.requireNonNull(stageCode, "stageCode must not be null");
        Objects.requireNonNull(input, "input must not be null");
    }
}
