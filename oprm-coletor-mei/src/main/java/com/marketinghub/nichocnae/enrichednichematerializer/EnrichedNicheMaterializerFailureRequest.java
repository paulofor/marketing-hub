package com.marketinghub.nichocnae.enrichednichematerializer;

/** Payload de falha enviado ao backend quando a etapa final não consegue materializar o nicho. */
public record EnrichedNicheMaterializerFailureRequest(String errorMessage) {}
