package com.marketinghub.feo.fabricacaov1.pipeline;

import java.util.Map;

/**
 * Representa um artefato auditavel produzido ou usado por uma etapa.
 */
public record StageArtifact(
        String type,
        String name,
        String contentType,
        byte[] content,
        String sha256,
        Map<String, Object> metadata) {
}
