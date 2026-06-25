package com.marketinghub.mois.dossiev1.pipeline;

import java.time.Instant;

/** Representa um artefato auditável produzido por uma etapa do pipeline de dossiê MOIS v1. */
public record StageArtifact(String type, String storageKey, String payload, Instant createdAt) {
}
