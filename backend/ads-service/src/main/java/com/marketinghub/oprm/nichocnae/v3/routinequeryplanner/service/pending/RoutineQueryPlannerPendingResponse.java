package com.marketinghub.oprm.nichocnae.v3.routinequeryplanner.service.pending;

/** Item pendente entregue ao executor para a etapa routine-query-planner. */
public record RoutineQueryPlannerPendingResponse(Long stageExecutionId, String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {}
