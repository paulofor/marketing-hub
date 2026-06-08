package com.marketinghub.worker.pipeline;

/** Responsabilidade: resumir o resultado operacional do processamento de uma execução de etapa. */
public record StageWorkerResult(String idJob, boolean success, String errorMessage) {
    /** Cria resultado de sucesso para uma execução processada. */
    public static StageWorkerResult success(String idJob) {
        return new StageWorkerResult(idJob, true, null);
    }

    /** Cria resultado de falha para uma execução processada. */
    public static StageWorkerResult failure(String idJob, Throwable error) {
        return new StageWorkerResult(idJob, false, error != null ? error.getMessage() : "Unknown error");
    }
}
