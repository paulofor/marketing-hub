package com.marketinghub.worker.pipeline;

import java.time.Instant;
import java.util.List;

/** Responsabilidade: consolidar a quantidade de execuções processadas pelo ciclo do worker. */
public record ProcessingSummary(boolean enabled, int total, int succeeded, int failed, Instant processedAt) {
    /** Consolida resultados individuais do worker em um resumo operacional. */
    public static ProcessingSummary from(List<StageWorkerResult> results) {
        int succeeded = (int) results.stream().filter(StageWorkerResult::success).count();
        int failed = results.size() - succeeded;
        return new ProcessingSummary(true, results.size(), succeeded, failed, Instant.now());
    }
}
