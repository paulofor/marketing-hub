package com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.createStageExecution;

/** Contrato enviado pelo executor para registrar uma pendência da etapa knowledge-accumulator do NichoCNAE v2. */
public record KnowledgeAccumulatorCreateRequest(
        String jobId,
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        Integer attemptNumber,
        Integer knowledgeVersion,
        Boolean materializationEnabled,
        String inputPayload) {}
