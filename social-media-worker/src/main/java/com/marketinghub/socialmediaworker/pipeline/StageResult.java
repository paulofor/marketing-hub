package com.marketinghub.socialmediaworker.pipeline;

/**
 * Representa o resultado tecnico e funcional de uma etapa executada.
 */
public record StageResult<O>(boolean success, O output, String errorCategory, String errorMessage) {

    /**
     * Cria um resultado de sucesso para a etapa.
     */
    public static <O> StageResult<O> success(O output) {
        return new StageResult<>(true, output, null, null);
    }

    /**
     * Cria um resultado de falha controlada para a etapa.
     */
    public static <O> StageResult<O> failure(String errorCategory, String errorMessage) {
        return new StageResult<>(false, null, errorCategory, errorMessage);
    }
}
