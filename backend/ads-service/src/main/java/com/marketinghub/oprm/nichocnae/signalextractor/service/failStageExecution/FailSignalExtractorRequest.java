package com.marketinghub.oprm.nichocnae.signalextractor.service.failStageExecution;

/** Representa a falha operacional registrada para a extração de sinais de um snapshot curto. */
public record FailSignalExtractorRequest(String errorMessage) {}
