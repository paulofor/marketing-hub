package com.marketinghub.nichocnae.pipeline;

/** Define o contrato genérico para persistência de artefatos auditáveis das etapas do pipeline nichocnae. */
public interface ArtifactStore {

    /** Armazena um artefato e devolve sua referência completa para auditoria da etapa. */
    StageArtifact store(StageArtifact artifact, byte[] content);
}
