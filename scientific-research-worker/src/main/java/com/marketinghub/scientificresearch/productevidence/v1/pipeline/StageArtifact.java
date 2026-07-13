package com.marketinghub.scientificresearch.productevidence.v1.pipeline;

/**
 * Representa um artefato auditável produzido por uma etapa.
 */
public record StageArtifact(String name, String mediaType, Object content) {
}
