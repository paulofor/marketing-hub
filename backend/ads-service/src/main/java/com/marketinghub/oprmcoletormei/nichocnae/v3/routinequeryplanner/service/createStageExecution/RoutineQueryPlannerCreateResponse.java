package com.marketinghub.oprmcoletormei.nichocnae.v3.routinequeryplanner.service.createStageExecution;

/** Resposta de criação de execução pendente da etapa routine-query-planner. */
public record RoutineQueryPlannerCreateResponse(Long stageExecutionId, String jobId, String cnaeCode, String stageCode, String status) {}
