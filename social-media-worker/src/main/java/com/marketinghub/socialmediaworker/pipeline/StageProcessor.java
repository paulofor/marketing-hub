package com.marketinghub.socialmediaworker.pipeline;

/**
 * Define o contrato comum de processamento de uma etapa concreta.
 */
public interface StageProcessor<I, O> {

    /**
     * Processa uma entrada de etapa e devolve a saida funcional estruturada.
     */
    O process(StageContext context, I input);
}
