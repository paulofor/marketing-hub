package com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.failStageExecution;

/** Contrato retornado ao executor após registrar falha ou retry técnico do filtro de segurança. */
public record SourceSafetyFilterFailureResponse(
        String stageExecutionId,
        String status,
        String retryStageExecutionId,
        Integer attemptNumber,
        Integer technicalRetryNumber) {}
