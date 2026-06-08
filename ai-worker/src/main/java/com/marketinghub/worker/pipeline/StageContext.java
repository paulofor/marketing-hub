package com.marketinghub.worker.pipeline;

import java.util.Map;
import java.util.Objects;

/** Responsabilidade: agrupar execução, entrada, store de artefatos e configuração para um processor de etapa. */
public record StageContext<I>(
        StageExecution<I> execution,
        I input,
        ArtifactStore artifactStore,
        Map<String, Object> config
) {
    /** Garante que o processor receba execução, entrada, artifact store e configuração não nulos. */
    public StageContext {
        Objects.requireNonNull(execution, "execution must not be null");
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        config = config == null ? Map.of() : Map.copyOf(config);
    }
}
