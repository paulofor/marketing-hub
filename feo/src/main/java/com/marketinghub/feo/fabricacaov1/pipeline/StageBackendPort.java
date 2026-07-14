package com.marketinghub.feo.fabricacaov1.pipeline;

import java.util.List;

/**
 * Define a comunicacao do pipeline com a fonte de verdade do backend.
 */
public interface StageBackendPort {

    /**
     * Busca execucoes pendentes pelo endpoint pending canonico.
     */
    <I> List<StageExecution<I>> fetchPending(StageCode stageCode, int limit);

    /**
     * Publica sucesso funcional da etapa no backend.
     */
    <O> void reportResult(StageExecution<?> execution, StageResult<O> result);

    /**
     * Publica falha tecnica ou funcional no backend.
     */
    void reportFailure(StageExecution<?> execution, Exception error);
}
