package com.marketinghub.worker.openai.core.model;

import java.time.Instant;

public record StageWorkerResult(
        String idJob,
        boolean success,
        String errorType,
        String errorMessage,
        Instant finishedAt
) {
    public static StageWorkerResult success(String idJob) {
        return new StageWorkerResult(idJob, true, null, null, Instant.now());
    }

    public static StageWorkerResult failure(String idJob, Throwable error) {
        String type = error == null ? "UnknownError" : error.getClass().getSimpleName();
        String message = error == null ? "Unknown error" : error.getMessage();
        return new StageWorkerResult(idJob, false, type, message, Instant.now());
    }
}
