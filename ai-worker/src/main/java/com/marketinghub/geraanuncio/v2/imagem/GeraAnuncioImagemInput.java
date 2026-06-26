package com.marketinghub.geraanuncio.v2.imagem;

import java.time.Instant;
import java.util.Map;

/** Responsabilidade: transportar a execução pendente recebida do backend para a etapa Imagem. */
public record GeraAnuncioImagemInput(
        String stageExecutionId,
        Long experimentId,
        String jobId,
        Instant requestedAt,
        Map<String, Object> context,
        Map<String, Object> previousArtifacts) {}
