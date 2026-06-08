package com.marketinghub.worker.pipeline;

import java.util.List;
import java.util.Map;

/** Responsabilidade: transportar saída estruturada, artefatos e métricas de uma execução de etapa. */
public record StageResult<O>(
        O output,
        List<StageArtifact> artifacts,
        Map<String, Object> metrics
) {
    /** Normaliza listas e mapas opcionais para simplificar consumidores do resultado. */
    public StageResult {
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
        metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
    }
}
