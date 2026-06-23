package com.marketinghub.opsmonitor.pipeline;

/** Define o contrato para armazenamento auditável de artefatos da execução. */
public interface ArtifactStore {

    /** Armazena um artefato e retorna uma referência estável para auditoria. */
    String store(StageContext context, StageArtifact artifact);
}
