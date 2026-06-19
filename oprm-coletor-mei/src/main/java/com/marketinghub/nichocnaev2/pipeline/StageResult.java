package com.marketinghub.nichocnaev2.pipeline;

import java.util.List;
import java.util.Map;

/** Representa a saída estruturada e auditável produzida por um processor de etapa. */
public record StageResult(String status, Map<String, Object> output, List<StageArtifact> artifacts) {
    /** Normaliza coleções nulas para preservar contrato simples entre etapas e backend. */
    public StageResult {
        output = output == null ? Map.of() : Map.copyOf(output);
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }
}
