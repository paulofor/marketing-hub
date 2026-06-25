package com.marketinghub.nichocnaev3.pipeline;

/** Executor genérico que roda uma etapa concreta sem conhecer seu pacote interno. */
public final class PipelineWorker {
    private final StageProcessor processor;

    /** Inicializa o worker com o processor plugável da etapa. */
    public PipelineWorker(StageProcessor processor) {
        this.processor = processor;
    }

    /** Executa uma etapa concreta e retorna seu resultado auditável. */
    public StageResult run(StageContext context) {
        return processor.process(context);
    }
}
