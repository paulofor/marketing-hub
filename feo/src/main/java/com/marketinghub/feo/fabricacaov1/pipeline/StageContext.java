package com.marketinghub.feo.fabricacaov1.pipeline;

/**
 * Entrega para uma etapa os dados da execucao e o mecanismo de artefatos.
 */
public record StageContext<I>(StageExecution<I> execution, I input, ArtifactStore artifactStore) {
}
