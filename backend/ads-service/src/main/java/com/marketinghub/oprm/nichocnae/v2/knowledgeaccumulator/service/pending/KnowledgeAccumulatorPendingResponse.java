package com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.pending;

/** Contrato entregue ao executor com a pendência da etapa knowledge-accumulator do NichoCNAE v2. */
public record KnowledgeAccumulatorPendingResponse(
        String stageExecutionId,
        String jobId,
        String cnaeCode,
        String cnaeDescription,
        Long researchCycleId,
        Long sourceNicheId,
        Integer attemptNumber,
        Integer technicalRetryNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        String inputPayload) {}
