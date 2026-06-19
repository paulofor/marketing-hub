package com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.completeStageExecution;

/** Contrato devolvido após o backend registrar a conclusão adaptive-query-planner do NichoCNAE v2. */
public record AdaptiveQueryPlannerCompletionResponse(
        String stageExecutionId,
        String status,
        String nextStageCode,
        Integer plannedQueryCount,
        Integer reusedQueryCount,
        Integer skippedQueryCount) {}
