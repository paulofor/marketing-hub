package com.marketinghub.worker.openai.core.model;

import java.time.Instant;
import java.util.List;

public record ProcessingSummary(
        boolean enabled,
        int total,
        int succeeded,
        int failed,
        Instant finishedAt
) {
    public static ProcessingSummary from(List<StageWorkerResult> results) {
        int succeeded = 0;
        int failed = 0;

        for (StageWorkerResult result : results) {
            if (result.success()) {
                succeeded++;
            } else {
                failed++;
            }
        }

        return new ProcessingSummary(true, results.size(), succeeded, failed, Instant.now());
    }

    public static ProcessingSummary disabled() {
        return new ProcessingSummary(false, 0, 0, 0, Instant.now());
    }
}
