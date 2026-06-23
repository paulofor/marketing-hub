package com.marketinghub.opsmonitor.pipeline;

/** Representa evidência auditável produzida por uma etapa do worker. */
public record StageArtifact(String name, String type, String payload) {}
