package com.marketinghub.worker.pipeline;

/** Responsabilidade: definir o contrato único de execução de uma etapa concreta do pipeline. */
public interface StageProcessor<I, O> {
    /** Processa a entrada da etapa e retorna saída estruturada com artefatos e métricas. */
    StageResult<O> process(StageContext<I> context);
}
