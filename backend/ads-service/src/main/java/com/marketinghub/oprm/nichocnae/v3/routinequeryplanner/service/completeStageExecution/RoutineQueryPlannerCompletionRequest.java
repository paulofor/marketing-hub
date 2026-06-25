package com.marketinghub.oprm.nichocnae.v3.routinequeryplanner.service.completeStageExecution;

/** Request de conclusão da etapa routine-query-planner reportada pelo executor. */
public record RoutineQueryPlannerCompletionRequest(String outputPayload, String nextStageCode) {}
