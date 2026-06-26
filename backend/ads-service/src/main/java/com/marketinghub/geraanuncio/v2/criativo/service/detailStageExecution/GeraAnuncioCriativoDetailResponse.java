package com.marketinghub.geraanuncio.v2.criativo.service.detailStageExecution;

import java.time.Instant;
import java.util.Map;

/** Detalhe auditável de uma execução da etapa Criativo do GeraAnuncio v2. */
public record GeraAnuncioCriativoDetailResponse(String stageExecutionId, String jobId, String status, Instant updatedAt, Map<String, Object> reportData) {}
