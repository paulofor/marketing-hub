package com.marketinghub.nichocnae.pipeline;

/** Define o contrato genérico que toda etapa concreta do pipeline nichocnae deve implementar. */
public interface StageProcessor<I, O> {

    /** Processa uma execução tipada e devolve a saída estruturada com métricas e artefatos. */
    StageResult<O> process(StageContext<I> context);
}
