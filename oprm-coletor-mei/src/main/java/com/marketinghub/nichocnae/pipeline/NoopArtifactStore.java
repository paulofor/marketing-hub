package com.marketinghub.nichocnae.pipeline;

import org.springframework.stereotype.Component;

/** Mantém um armazenamento nulo para etapas que ainda não geram artefatos persistentes no MVP. */
@Component
public class NoopArtifactStore implements ArtifactStore {

    /** Devolve o próprio artefato porque a etapa zero não publica conteúdo nem payload final persistente. */
    @Override
    public StageArtifact store(StageArtifact artifact, byte[] content) {
        return artifact;
    }
}
