package com.marketinghub.scientificresearch.productevidence.v1.pipeline;

import java.util.List;

/**
 * Porta de comunicação do núcleo com o backend principal.
 */
public interface StageBackendPort {

    /**
     * Busca execuções pendentes de uma etapa no backend.
     */
    List<StageContext> fetchPending(StageCode stageCode);

    /**
     * Reporta o resultado de uma execução ao backend.
     */
    void reportResult(StageContext context, StageResult result);

    /**
     * Reporta uma falha técnica de execução ao backend.
     */
    void reportFailure(StageContext context, Exception exception);
}
