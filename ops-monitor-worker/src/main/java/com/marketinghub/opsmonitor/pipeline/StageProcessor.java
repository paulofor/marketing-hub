package com.marketinghub.opsmonitor.pipeline;

/** Define o contrato comum de processamento para qualquer etapa plugável. */
public interface StageProcessor<I, O> {

    /** Processa uma entrada da etapa usando o contexto recebido. */
    O process(StageContext context, I input);
}
