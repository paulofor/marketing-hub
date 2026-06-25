package com.marketinghub.mois.dossiev1.pipeline;

/** Define a porta de armazenamento de artefatos auditáveis do pipeline de dossiê MOIS v1. */
public interface ArtifactStore {
    /** Persiste ou encaminha um artefato produzido por uma etapa concreta. */
    StageArtifact save(StageContext context, StageArtifact artifact);
}
