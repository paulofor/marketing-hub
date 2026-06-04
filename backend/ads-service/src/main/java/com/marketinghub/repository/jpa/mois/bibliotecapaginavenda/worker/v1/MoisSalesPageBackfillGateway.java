package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1;

/**
 * Define as operações de persistência necessárias para o backfill do modelo consolidado de páginas de venda MOIS.
 */
public interface MoisSalesPageBackfillGateway {

    /**
     * Verifica se o schema necessário ao backfill está disponível no banco atual.
     */
    boolean hasBackfillTables();

    /**
     * Conta páginas consolidadas no modelo novo.
     */
    long countSalesPages();

    /**
     * Conta execuções já gravadas no histórico novo.
     */
    long countJobExecutions();

    /**
     * Conta URLs consolidadas do modelo legado que devem ser migradas.
     */
    long countLegacyUrlIngests();

    /**
     * Insere ou atualiza páginas consolidadas no modelo novo.
     */
    int backfillSalesPages();

    /**
     * Migra o último job de processamento legado de cada página.
     */
    int backfillLatestProcessingJobs();

    /**
     * Migra a última análise legada de cada página.
     */
    int backfillLatestAnalyses();

    /**
     * Migra o último snapshot legado de cada página.
     */
    int backfillLatestSnapshots();

    /**
     * Migra a última captura bruta vinculada à referência coletada.
     */
    int backfillLatestCollectedReferenceHtmlCaptures();

    /**
     * Atualiza os ponteiros das páginas para a execução mais recente migrada.
     */
    int updateLastJobExecutionPointers();
}
