package com.marketinghub.nichocnaev3.pipeline;

import java.util.List;
import java.util.Map;

/** Resultado funcional produzido por uma etapa do NichoCNAE v3. */
public record StageResult(String status, Map<String, Object> output, List<StageArtifact> artifacts) {}
