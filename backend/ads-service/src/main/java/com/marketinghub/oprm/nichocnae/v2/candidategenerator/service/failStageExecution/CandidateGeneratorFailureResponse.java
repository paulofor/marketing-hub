package com.marketinghub.oprm.nichocnae.v2.candidategenerator.service.failStageExecution;

/** Contrato retornado ao executor após registrar falha e possível retry técnico do NichoCNAE v2. */
public record CandidateGeneratorFailureResponse(
        String stageExecutionId,
        String status,
        String retryStageExecutionId,
        Integer attemptNumber,
        Integer technicalRetryNumber) {}
