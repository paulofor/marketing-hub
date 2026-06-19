package com.marketinghub.nichocnaev2.pipeline;

/** Coordena a execução local de uma etapa concreta sem conhecer implementação específica de etapa. */
public final class PipelineWorker {
    private final StageProcessor processor;

    /** Recebe o processor plugável que será executado pelo worker genérico. */
    public PipelineWorker(StageProcessor processor) {
        this.processor = processor;
    }

    /** Executa o processor configurado e retorna o resultado para posterior callback ao backend. */
    public StageResult run(StageContext context) {
        return processor.process(context);
    }
}
