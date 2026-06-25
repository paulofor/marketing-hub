package com.marketinghub.oprm.nichocnae.v3.personaroutinematerializer.service.completeStageExecution;

/** Request de conclusão da etapa persona-routine-materializer reportada pelo executor. */
public record PersonaRoutineMaterializerCompletionRequest(String outputPayload, String nextStageCode) {}
