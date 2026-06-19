package com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.failStageExecution;

/** Resposta de falha da etapa enriched-niche-materializer, incluindo retry técnico quando aplicável. */
public record EnrichedNicheMaterializerFailureResponse(
        String stageExecutionId,
        String status,
        String retryStageExecutionId,
        Integer attemptNumber,
        Integer technicalRetryNumber) {}
