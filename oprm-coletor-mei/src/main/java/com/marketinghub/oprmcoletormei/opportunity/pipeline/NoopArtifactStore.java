package com.marketinghub.oprmcoletormei.opportunity.pipeline;

/** Implementação neutra para etapas determinísticas que ainda não persistem artefatos externos. */
public class NoopArtifactStore implements ArtifactStore {
    /** Retorna o próprio artefato recebido sem gravar em tecnologia externa. */
    @Override
    public StageArtifact store(StageArtifact artifact) {
        return artifact;
    }
}
