package com.marketinghub.oprm.nichocnae.sourcesearcher.service.failStageExecution;

/** Representa o payload usado para registrar falha operacional da etapa três de busca. */
public record FailSourceSearcherRequest(String errorMessage) {}
