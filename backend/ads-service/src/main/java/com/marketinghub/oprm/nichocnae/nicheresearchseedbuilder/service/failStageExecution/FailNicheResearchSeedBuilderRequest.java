package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.failStageExecution;

/** Contrato para registrar o motivo de falha operacional da etapa dois. */
public record FailNicheResearchSeedBuilderRequest(String errorMessage, String errorDetail) {}
