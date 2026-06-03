package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline;

import java.util.Map;

/** Descreve um artefato bruto ou derivado gerado por uma etapa de pipeline. */
public record StageArtifact(
        String type,
        String name,
        String contentType,
        String storageKey,
        String sha256,
        long sizeBytes,
        Map<String, Object> metadata
) {
}
