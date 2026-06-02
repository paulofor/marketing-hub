package com.marketinghub.oprm.nichocnae.sourcefetcher.service.failStageExecution;

/** Representa o motivo de rejeição ou falha de coleta de uma fonte candidata da etapa quatro. */
public record FailSourceFetcherRequest(String rejectionReason, Integer relevanceScore) {}
