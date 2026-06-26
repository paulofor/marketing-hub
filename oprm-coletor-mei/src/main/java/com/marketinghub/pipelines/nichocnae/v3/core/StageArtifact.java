package com.marketinghub.pipelines.nichocnae.v3.core;

/** Artefato auditável produzido por uma etapa do NichoCNAE v3. */
public record StageArtifact(String type, String uri, String summary) {}
