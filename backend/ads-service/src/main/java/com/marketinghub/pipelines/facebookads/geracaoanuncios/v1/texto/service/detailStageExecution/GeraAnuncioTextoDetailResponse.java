package com.marketinghub.pipelines.facebookads.geracaoanuncios.v1.texto.service.detailStageExecution;

import java.time.Instant;
import java.util.Map;

/** Detalhe auditável de uma execução da etapa Texto do GeraAnuncio v2. */
public record GeraAnuncioTextoDetailResponse(String stageExecutionId, String jobId, String status, Instant updatedAt, Map<String, Object> reportData) {}
