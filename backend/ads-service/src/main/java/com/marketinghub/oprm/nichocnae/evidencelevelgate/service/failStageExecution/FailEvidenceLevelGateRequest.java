package com.marketinghub.oprm.nichocnae.evidencelevelgate.service.failStageExecution;

/** Contrato usado pelo executor externo para registrar falha técnica da etapa E0-E5. */
public record FailEvidenceLevelGateRequest(String errorMessage) {}
