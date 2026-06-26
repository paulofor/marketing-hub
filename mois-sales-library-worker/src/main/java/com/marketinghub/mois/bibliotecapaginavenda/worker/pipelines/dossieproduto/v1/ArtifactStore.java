package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1;

/** Define a porta de gravação de artefatos auditáveis produzidos pelas etapas. */
public interface ArtifactStore {

    /** Armazena o payload e retorna a chave de auditoria do artefato. */
    String store(long jobId, String artifactType, String payload);
}
