package com.marketinghub.pipelines.geracaoanuncios.v1.texto;

import com.marketinghub.worker.pipeline.StageArtifact;
import com.marketinghub.worker.pipeline.StageContext;
import com.marketinghub.worker.pipeline.StageResult;
import java.util.List;
import java.util.Map;

/** Responsabilidade: transformar a saída validada da etapa Texto em resultado auditável do pipeline. */
public class GeraAnuncioTextoResponseHandler {
    /** Registra artefato auditável e devolve o resultado completo para o worker genérico. */
    public StageResult<GeraAnuncioTextoOutput> handle(
            StageContext<GeraAnuncioTextoInput> context,
            String requestPayload,
            GeraAnuncioTextoOutput output) {
        StageArtifact requestArtifact = context.artifactStore().save(
                "TEXTO_REQUEST",
                "geracaoanuncios-v1-texto-request.json",
                "application/json",
                requestPayload,
                Map.of("jobId", context.input().jobId(), "stageExecutionId", context.input().stageExecutionId()));
        return new StageResult<>(output, List.of(requestArtifact), Map.of("artifactCount", 1));
    }
}
