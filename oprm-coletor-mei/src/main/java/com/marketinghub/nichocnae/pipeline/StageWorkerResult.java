package com.marketinghub.nichocnae.pipeline;

/** Resume o sucesso ou falha de uma execução do worker genérico do pipeline nichocnae. */
public record StageWorkerResult(
        String idJob,
        boolean success,
        String errorMessage) {

    /** Cria um resultado de sucesso para a execução informada. */
    public static StageWorkerResult success(String idJob) {
        return new StageWorkerResult(idJob, true, null);
    }

    /** Cria um resultado de falha preservando a mensagem operacional do erro. */
    public static StageWorkerResult failure(String idJob, Exception error) {
        return new StageWorkerResult(idJob, false, error.getMessage());
    }
}
