package com.marketinghub.oprm.nichocnae.routinequalitygate.service.failStageExecution;

/** Payload usado para registrar falha operacional da etapa sete do OPRM NichoCNAE. */
public record FailRoutineQualityGateRequest(String errorMessage) {}
