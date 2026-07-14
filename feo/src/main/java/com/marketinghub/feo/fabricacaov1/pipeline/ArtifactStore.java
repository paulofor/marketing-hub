package com.marketinghub.feo.fabricacaov1.pipeline;

/**
 * Define como uma etapa registra artefatos auditaveis.
 */
public interface ArtifactStore {

    /**
     * Armazena um artefato e devolve a representacao persistivel.
     */
    StageArtifact store(String type, String name, String contentType, byte[] content);
}
