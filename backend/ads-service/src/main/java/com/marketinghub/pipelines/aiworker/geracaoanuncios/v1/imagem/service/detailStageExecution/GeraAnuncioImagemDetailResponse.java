package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.detailStageExecution;

import java.time.Instant;
import java.util.Map;

/** Detalhe auditável de uma execução da etapa Imagem do GeraAnuncio v2. */
public record GeraAnuncioImagemDetailResponse(String stageExecutionId, String jobId, String status, Instant updatedAt, Map<String, Object> reportData) {}
