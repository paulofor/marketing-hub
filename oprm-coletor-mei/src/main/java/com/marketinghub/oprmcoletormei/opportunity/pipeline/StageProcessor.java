package com.marketinghub.oprmcoletormei.opportunity.pipeline;

/** Contrato genérico que toda etapa concreta do fluxo CNAE de oportunidade deve implementar. */
public interface StageProcessor<I, O> {
    /** Processa uma entrada de etapa e retorna uma saída estruturada com rastreabilidade. */
    StageResult<O> process(StageContext<I> context);
}
