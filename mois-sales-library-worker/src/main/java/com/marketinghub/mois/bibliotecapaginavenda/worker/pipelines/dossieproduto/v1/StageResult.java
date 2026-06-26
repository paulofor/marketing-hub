package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1;

import java.util.List;
import java.util.Map;

/** Representa a saída funcional estruturada de uma etapa do dossiê do produto. */
public record StageResult(String status, Map<String, Object> output, List<StageArtifact> artifacts) {

    /** Cria um resultado concluído com saída funcional e artefatos auditáveis. */
    public static StageResult done(Map<String, Object> output, List<StageArtifact> artifacts) {
        return new StageResult("DONE", output, artifacts);
    }
}
