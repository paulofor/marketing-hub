package com.marketinghub.mois.dossiev1.pipeline;

/** Define o contrato plugável que toda etapa concreta do pipeline de dossiê MOIS v1 deve implementar. */
public interface StageProcessor {
    /** Informa o nome canônico da etapa executada pelo processor. */
    String stageName();

    /** Processa uma etapa a partir do contexto recebido do backend e devolve saída estruturada. */
    StageResult process(StageContext context);
}
