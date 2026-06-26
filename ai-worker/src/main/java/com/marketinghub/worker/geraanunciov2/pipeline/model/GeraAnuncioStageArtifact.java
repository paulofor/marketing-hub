package com.marketinghub.worker.geraanunciov2.pipeline.model;

import java.util.Map;

/** Responsabilidade: representar artefato auditável produzido por uma etapa do GeraAnuncio v2. */
public record GeraAnuncioStageArtifact(String name, String type, Map<String, Object> payload) {}
