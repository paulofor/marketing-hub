package com.marketinghub.worker.pipeline;

import java.util.Map;

/** Responsabilidade: referenciar um artefato auditável usado ou gerado por uma etapa do pipeline. */
public record StageArtifact(
        String type,
        String name,
        String contentType,
        String storageKey,
        String sha256,
        Map<String, Object> metadata
) {
    /** Normaliza metadados opcionais para evitar nulos na auditoria do pipeline. */
    public StageArtifact {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
