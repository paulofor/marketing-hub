package com.marketinghub.oprm.nichocnae.v3.dailytaskssynthesizer.service.createStageExecution;

/** Resposta de criação de execução pendente da etapa daily-tasks-synthesizer. */
public record DailyTasksSynthesizerCreateResponse(Long stageExecutionId, String jobId, String cnaeCode, String stageCode, String status) {}
