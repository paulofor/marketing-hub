package com.marketinghub.oprmcoletormei.opportunity.pipeline;

import java.util.Map;

/** Artefato auditável produzido ou consumido por uma etapa do fluxo CNAE de oportunidade. */
public record StageArtifact(
        String type,
        String name,
        String contentType,
        String storageKey,
        String sha256,
        Map<String, Object> metadata) {}
