package com.marketinghub.oprm.nichocnae.v2.commercialevidencegate.service.failStageExecution;

/** Resposta de falha da etapa commercial-evidence-gate, incluindo retry técnico quando aplicável. */
public record CommercialEvidenceGateFailureResponse(
        String stageExecutionId,
        String status,
        String retryStageExecutionId,
        Integer attemptNumber,
        Integer technicalRetryNumber) {}
