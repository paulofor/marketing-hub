package com.marketinghub.geraanuncio.v2.texto.service.listStageExecutions;

import java.time.Instant;

/** Resumo de execução da etapa Texto do GeraAnuncio v2 para relatórios. */
public record GeraAnuncioTextoExecutionSummaryResponse(String stageExecutionId, Long experimentId, String jobId, String status, Instant updatedAt) {}
