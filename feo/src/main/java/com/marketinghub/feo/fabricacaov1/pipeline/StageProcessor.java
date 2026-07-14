package com.marketinghub.feo.fabricacaov1.pipeline;

/**
 * Contrato que toda etapa concreta da FEO deve implementar.
 */
public interface StageProcessor<I, O> {

    /**
     * Retorna o codigo canonico da etapa.
     */
    StageCode stageCode();

    /**
     * Executa a etapa a partir do contexto recebido.
     */
    StageResult<O> process(StageContext<I> context);
}
