package com.marketinghub.oprm.nichocnae.v3.routinesignalextractor.service.createStageExecution;

/** Resposta de criação de execução pendente da etapa routine-signal-extractor. */
public record RoutineSignalExtractorCreateResponse(Long stageExecutionId, String jobId, String cnaeCode, String stageCode, String status) {}
