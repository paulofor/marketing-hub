package com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.completeStageExecution;

/** Contrato devolvido após o backend registrar a conclusão knowledge-accumulator do NichoCNAE v2. */
public record KnowledgeAccumulatorCompletionResponse(
        String stageExecutionId,
        String status,
        String nextStageCode,
        Integer knowledgeVersion,
        Integer validatedFactCount,
        Integer acceptedSourceCount,
        Integer rejectedSourceCount) {}
