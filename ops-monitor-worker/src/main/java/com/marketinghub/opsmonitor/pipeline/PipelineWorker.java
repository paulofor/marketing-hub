package com.marketinghub.opsmonitor.pipeline;

/** Orquestra a execução genérica de etapas sem conhecer implementações concretas. */
public class PipelineWorker<I, O> {
    private final StageProcessor<I, O> processor;

    /** Recebe o processador plugável que executará a etapa concreta. */
    public PipelineWorker(StageProcessor<I, O> processor) {
        this.processor = processor;
    }

    /** Executa a etapa configurada com o contexto e a entrada informados. */
    public O run(StageContext context, I input) {
        return processor.process(context, input);
    }
}
