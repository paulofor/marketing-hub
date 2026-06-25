package com.marketinghub.nichocnaev3.pipeline;

import java.util.Map;

/** Contexto genérico recebido por uma etapa plugável do NichoCNAE v3. */
public record StageContext(String jobId, String stageExecutionId, Map<String, Object> input) {}
