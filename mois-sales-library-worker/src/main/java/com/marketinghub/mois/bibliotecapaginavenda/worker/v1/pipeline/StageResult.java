package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline;

import java.util.List;
import java.util.Map;

/** Consolida a saída estruturada, artefatos e métricas produzidos por uma etapa. */
public record StageResult<O>(
        O output,
        List<StageArtifact> artifacts,
        Map<String, Object> metrics
) {
}
