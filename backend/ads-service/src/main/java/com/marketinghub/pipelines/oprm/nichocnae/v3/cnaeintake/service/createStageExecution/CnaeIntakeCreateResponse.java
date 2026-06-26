package com.marketinghub.pipelines.oprm.nichocnae.v3.cnaeintake.service.createStageExecution;

/** Resposta de criação de execução pendente da etapa cnae-intake. */
public record CnaeIntakeCreateResponse(Long stageExecutionId, String jobId, String cnaeCode, String stageCode, String status) {}
