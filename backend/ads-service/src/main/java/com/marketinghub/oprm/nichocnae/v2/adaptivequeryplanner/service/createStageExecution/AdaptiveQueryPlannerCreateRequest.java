package com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.createStageExecution;

/** Contrato enviado pelo executor para registrar uma pendência da etapa adaptive-query-planner do NichoCNAE v2. */
public record AdaptiveQueryPlannerCreateRequest(
        String jobId,
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        Integer attemptNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        String inputPayload) {}
