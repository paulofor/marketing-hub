package com.marketinghub.oprm.nichocnae.v3.routinesignalextractor.service.pending;

/** Item pendente entregue ao executor para a etapa routine-signal-extractor. */
public record RoutineSignalExtractorPendingResponse(Long stageExecutionId, String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {}
