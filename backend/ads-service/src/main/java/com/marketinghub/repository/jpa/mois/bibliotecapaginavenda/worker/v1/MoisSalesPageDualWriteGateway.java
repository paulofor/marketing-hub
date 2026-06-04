package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1;

/**
 * Define operações de escrita dupla entre o modelo legado da biblioteca MOIS e as novas tabelas consolidadas.
 */
public interface MoisSalesPageDualWriteGateway {

    /**
     * Sincroniza uma URL ingerida e, quando informado, o job pendente recém-criado no modelo consolidado.
     */
    void syncUrlIngest(long urlIngestId, Long processingJobId);

    /**
     * Sincroniza o estado atual de um job de processamento legado no histórico consolidado.
     */
    void syncProcessingJob(long processingJobId);

    /**
     * Sincroniza a análise mais recente vinculada a uma página legada no modelo consolidado.
     */
    void syncLatestAnalysis(long urlIngestId);

    /**
     * Sincroniza um snapshot legado específico no histórico consolidado.
     */
    void syncSnapshot(long snapshotId);

    /**
     * Sincroniza uma captura bruta de referência coletada quando ela estiver vinculada a uma página consolidada.
     */
    void syncCollectedReferenceHtmlCapture(long captureId);
}
