package com.marketinghub.geraanuncio.v2.texto.service.pending;

import java.time.Instant;
import java.util.Map;

/** Resposta com uma execução pendente da etapa Texto do GeraAnuncio v2. */
public record GeraAnuncioTextoPendingResponse(
        String stageExecutionId,
        Long experimentId,
        String jobId,
        Instant requestedAt,
        Map<String, Object> context,
        Map<String, Object> previousArtifacts) {}
