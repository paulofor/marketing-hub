package com.marketinghub.pipelines.nichocnae.v3.core;

import java.util.List;
import java.util.Map;

/** Resultado funcional produzido por uma etapa do NichoCNAE v3. */
public record StageResult(String status, Map<String, Object> output, List<StageArtifact> artifacts) {}
