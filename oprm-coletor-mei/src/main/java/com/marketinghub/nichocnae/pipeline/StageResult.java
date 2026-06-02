package com.marketinghub.nichocnae.pipeline;

import java.util.List;
import java.util.Map;

/** Descreve a saída estruturada, artefatos e métricas produzidas por uma etapa do pipeline nichocnae. */
public record StageResult<O>(
        O output,
        List<StageArtifact> artifacts,
        Map<String, Object> metrics) {}
