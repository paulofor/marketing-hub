package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline;

/** Define o contrato de entrada de qualquer etapa concreta do pipeline. */
public interface StageProcessor<I, O> {

    /** Processa uma execução de etapa e retorna saída estruturada com artefatos auditáveis. */
    StageResult<O> process(StageContext<I> context) throws Exception;
}
