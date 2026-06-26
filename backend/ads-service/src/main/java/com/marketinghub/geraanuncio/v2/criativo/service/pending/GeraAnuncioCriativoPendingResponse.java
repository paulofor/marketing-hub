package com.marketinghub.geraanuncio.v2.criativo.service.pending;

import java.time.Instant;
import java.util.Map;

/** Resposta com uma execução pendente da etapa Criativo do GeraAnuncio v2. */
public record GeraAnuncioCriativoPendingResponse(
        String stageExecutionId,
        Long experimentId,
        String jobId,
        Instant requestedAt,
        Map<String, Object> context,
        Map<String, Object> previousArtifacts) {}
