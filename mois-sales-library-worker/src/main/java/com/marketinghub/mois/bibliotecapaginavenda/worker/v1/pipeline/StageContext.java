package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline;

/** Carrega a execução, entrada e persistência de artefatos disponíveis para uma etapa. */
public record StageContext<I>(
        StageExecution<I> execution,
        I input,
        ArtifactStore artifactStore
) {
}
