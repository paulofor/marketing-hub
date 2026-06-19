package com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.failStageExecution;

/** Contrato devolvido após o backend registrar falha adaptive-query-planner do NichoCNAE v2. */
public record AdaptiveQueryPlannerFailureResponse(
        String stageExecutionId,
        String status,
        String retryStageExecutionId,
        Integer attemptNumber,
        Integer technicalRetryNumber) {}
