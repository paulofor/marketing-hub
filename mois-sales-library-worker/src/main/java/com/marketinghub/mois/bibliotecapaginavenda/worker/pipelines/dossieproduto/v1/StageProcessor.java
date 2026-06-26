package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1;

/** Define o contrato plugável executado por cada etapa concreta do dossiê do produto. */
public interface StageProcessor {

    /** Informa o nome canônico da etapa concreta. */
    String stageName();

    /** Executa a etapa usando apenas o contexto persistido entregue pelo backend. */
    StageResult process(StageContext context);
}
