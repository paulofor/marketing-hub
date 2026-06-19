package com.marketinghub.nichocnaev2.pipeline;

/** Descreve um artefato auditável gerado por uma etapa do pipeline NichoCNAE versão 2. */
public record StageArtifact(String type, String storageKey, String description) {}
