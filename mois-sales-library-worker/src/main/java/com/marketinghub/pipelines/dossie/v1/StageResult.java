package com.marketinghub.pipelines.dossie.v1;

import java.util.List;
import java.util.Map;

/** Descreve a saída estruturada de uma etapa executada pelo pipeline de dossiê MOIS v1. */
public record StageResult(String status, Map<String, Object> output, List<StageArtifact> artifacts, String errorMessage) {
    /** Cria um resultado concluído com saída funcional e artefatos auditáveis. */
    public static StageResult done(Map<String, Object> output, List<StageArtifact> artifacts) {
        return new StageResult("DONE", output, artifacts, null);
    }
}
