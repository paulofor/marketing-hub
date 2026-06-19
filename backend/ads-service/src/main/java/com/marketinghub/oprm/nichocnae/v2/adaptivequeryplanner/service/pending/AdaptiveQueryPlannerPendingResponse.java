package com.marketinghub.oprm.nichocnae.v2.adaptivequeryplanner.service.pending;

/** Contrato entregue ao executor com a pendência da etapa adaptive-query-planner do NichoCNAE v2. */
public record AdaptiveQueryPlannerPendingResponse(
        String stageExecutionId,
        String jobId,
        String cnaeCode,
        Long sourceNicheId,
        Integer attemptNumber,
        Integer technicalRetryNumber,
        Integer knowledgeVersion,
        String inputPayload) {}
