package com.marketinghub.nichocnae.pipeline;

import java.util.Map;

/** Representa um artefato auditável produzido ou consumido por uma etapa do pipeline nichocnae. */
public record StageArtifact(
        String type,
        String name,
        String contentType,
        String storageKey,
        String sha256,
        Map<String, Object> metadata) {}
