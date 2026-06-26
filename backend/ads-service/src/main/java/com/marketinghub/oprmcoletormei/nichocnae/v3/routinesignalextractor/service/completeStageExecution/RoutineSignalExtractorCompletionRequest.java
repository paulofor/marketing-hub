package com.marketinghub.oprmcoletormei.nichocnae.v3.routinesignalextractor.service.completeStageExecution;

/** Request de conclusão da etapa routine-signal-extractor reportada pelo executor. */
public record RoutineSignalExtractorCompletionRequest(String outputPayload, String nextStageCode) {}
