package com.marketinghub.pipelines.oprm.nichocnae.v3.qualitygate.service.createStageExecution;

/** Resposta de criação de execução pendente da etapa quality-gate. */
public record QualityGateCreateResponse(Long stageExecutionId, String jobId, String cnaeCode, String stageCode, String status) {}
