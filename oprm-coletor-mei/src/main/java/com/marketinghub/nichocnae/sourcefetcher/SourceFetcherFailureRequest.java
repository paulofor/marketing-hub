package com.marketinghub.nichocnae.sourcefetcher;

/** Representa o payload de rejeição ou falha enviado ao backend para a etapa quatro. */
public record SourceFetcherFailureRequest(String rejectionReason, Integer relevanceScore) {}
