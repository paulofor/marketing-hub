package com.marketinghub.pipelines.facebookads.geracaoanuncios.v1.imagem.service.pending;

import java.time.Instant;
import java.util.Map;

/** Resposta com uma execução pendente da etapa Imagem do GeraAnuncio v2. */
public record GeraAnuncioImagemPendingResponse(
        String stageExecutionId,
        Long experimentId,
        String jobId,
        Instant requestedAt,
        Map<String, Object> context,
        Map<String, Object> previousArtifacts) {}
