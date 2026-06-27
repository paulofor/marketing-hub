package com.marketinghub.pipelines.geracaoanuncios.v1.imagem;

import com.marketinghub.worker.pipeline.StageArtifact;
import com.marketinghub.worker.pipeline.StageContext;
import com.marketinghub.worker.pipeline.StageResult;
import java.util.List;
import java.util.Map;

/** Responsabilidade: transformar a saída validada da etapa Imagem em resultado auditável do pipeline. */
public class GeraAnuncioImagemResponseHandler {
    /** Registra artefato auditável e devolve o resultado completo para o worker genérico. */
    public StageResult<GeraAnuncioImagemOutput> handle(
            StageContext<GeraAnuncioImagemInput> context,
            String requestPayload,
            GeraAnuncioImagemOutput output) {
        StageArtifact requestArtifact = context.artifactStore().save(
                "IMAGEM_REQUEST",
                "geracaoanuncios-v1-imagem-request.json",
                "application/json",
                requestPayload,
                Map.of("jobId", context.input().jobId(), "stageExecutionId", context.input().stageExecutionId()));
        return new StageResult<>(output, List.of(requestArtifact), Map.of("artifactCount", 1));
    }
}
