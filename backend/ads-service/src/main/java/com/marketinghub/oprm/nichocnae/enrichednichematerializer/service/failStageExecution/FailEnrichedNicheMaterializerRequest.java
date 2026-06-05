package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.failStageExecution;

/** Payload usado para registrar falha operacional da etapa final de materialização. */
public record FailEnrichedNicheMaterializerRequest(String errorMessage) {}
