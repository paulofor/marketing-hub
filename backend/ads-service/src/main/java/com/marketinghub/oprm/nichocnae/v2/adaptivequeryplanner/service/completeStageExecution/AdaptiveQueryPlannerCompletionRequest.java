package com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.completeStageExecution;

/** Contrato recebido do executor ao concluir a etapa adaptive-query-planner do NichoCNAE v2. */
public record AdaptiveQueryPlannerCompletionRequest(
        String planDecision,
        Integer plannedQueryCount,
        Integer reusedQueryCount,
        Integer skippedQueryCount,
        String outputPayload,
        String nextStageCode) {}
