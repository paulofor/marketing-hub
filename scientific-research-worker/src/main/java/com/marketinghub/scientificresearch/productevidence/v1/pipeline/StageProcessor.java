package com.marketinghub.scientificresearch.productevidence.v1.pipeline;

/**
 * Contrato que toda etapa concreta do pipeline deve implementar.
 */
public interface StageProcessor {

    /**
     * Retorna o código canônico da etapa processada.
     */
    StageCode stageCode();

    /**
     * Executa uma etapa com o contexto recebido do backend.
     */
    StageResult process(StageContext context);
}
