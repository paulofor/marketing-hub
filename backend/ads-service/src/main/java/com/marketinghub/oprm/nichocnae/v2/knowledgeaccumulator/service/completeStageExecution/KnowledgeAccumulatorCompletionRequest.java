package com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.completeStageExecution;

/** Contrato recebido do executor ao concluir a etapa knowledge-accumulator do NichoCNAE v2. */
public record KnowledgeAccumulatorCompletionRequest(
        Integer knowledgeVersion,
        Integer validatedFactCount,
        Integer acceptedSourceCount,
        Integer rejectedSourceCount,
        String outputPayload,
        String nextStageCode) {}
