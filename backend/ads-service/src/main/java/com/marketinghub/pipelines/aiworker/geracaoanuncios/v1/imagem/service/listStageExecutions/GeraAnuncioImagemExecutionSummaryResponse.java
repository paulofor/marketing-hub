package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.listStageExecutions;

import java.time.Instant;

/** Resumo de execução da etapa Imagem do GeraAnuncio v2 para relatórios. */
public record GeraAnuncioImagemExecutionSummaryResponse(String stageExecutionId, Long experimentId, String jobId, String status, Instant updatedAt) {}
