package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline;

/** Abstrai persistência de artefatos para manter o núcleo independente de tecnologias concretas. */
public interface ArtifactStore {

    /** Persiste um artefato textual e devolve a chave lógica de armazenamento. */
    String putText(long executionId, String artifactType, String contentType, String content);
}
