package com.marketinghub.pipelines.oprm.nichocnae.v3.personaroutinematerializer.service.failStageExecution;

/** Request de falha da etapa persona-routine-materializer reportada pelo executor. */
public record PersonaRoutineMaterializerFailureRequest(String errorMessage) {}
