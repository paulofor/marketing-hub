package com.marketinghub.geraanuncio.v2.criativo.service.listStageExecutions;

import java.time.Instant;

/** Resumo de execução da etapa Criativo do GeraAnuncio v2 para relatórios. */
public record GeraAnuncioCriativoExecutionSummaryResponse(String stageExecutionId, Long experimentId, String jobId, String status, Instant updatedAt) {}
