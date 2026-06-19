package com.marketinghub.oprm.nichocnae.v2.knowledgeaccumulator.service.failStageExecution;

/** Contrato devolvido após o backend registrar falha knowledge-accumulator do NichoCNAE v2. */
public record KnowledgeAccumulatorFailureResponse(
        String stageExecutionId,
        String status,
        String retryStageExecutionId,
        Integer attemptNumber,
        Integer technicalRetryNumber) {}
