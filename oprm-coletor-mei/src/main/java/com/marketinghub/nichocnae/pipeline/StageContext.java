package com.marketinghub.nichocnae.pipeline;

import java.util.Map;

/** Reúne os dados necessários para uma etapa concreta executar sem acoplar o núcleo à tecnologia usada. */
public record StageContext<I>(
        StageExecution<I> execution,
        I input,
        ArtifactStore artifactStore,
        Map<String, Object> config) {}
