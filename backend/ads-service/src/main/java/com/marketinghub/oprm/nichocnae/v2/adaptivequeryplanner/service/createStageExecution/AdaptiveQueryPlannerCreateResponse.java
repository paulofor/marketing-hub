package com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.createStageExecution;

/** Contrato devolvido após o backend gravar a pendência adaptive-query-planner do NichoCNAE v2. */
public record AdaptiveQueryPlannerCreateResponse(String stageExecutionId, String status, String stageCode) {}
