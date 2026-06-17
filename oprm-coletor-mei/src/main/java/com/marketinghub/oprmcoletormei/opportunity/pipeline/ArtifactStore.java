package com.marketinghub.oprmcoletormei.opportunity.pipeline;

/** Porta genérica para armazenar artefatos de etapas sem acoplar o núcleo a tecnologia concreta. */
public interface ArtifactStore {
    /** Armazena um artefato auditável e retorna a referência canônica gravada. */
    StageArtifact store(StageArtifact artifact);
}
