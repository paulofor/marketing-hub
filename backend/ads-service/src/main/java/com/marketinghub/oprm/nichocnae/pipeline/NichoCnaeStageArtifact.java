package com.marketinghub.oprm.nichocnae.pipeline;

import java.time.Instant;

/** Responsabilidade: representar um artefato auditável produzido por uma etapa do pipeline OPRM NichoCNAE. */
public record NichoCnaeStageArtifact(String type, String storageKey, String payload, Instant createdAt) {}
