package com.marketinghub.nichocnaev2.pipeline;

/** Contrato genérico implementado por cada etapa concreta do pipeline NichoCNAE versão 2. */
public interface StageProcessor {
    /** Executa a etapa usando somente o contexto recebido e devolve uma saída estruturada. */
    StageResult process(StageContext context);
}
