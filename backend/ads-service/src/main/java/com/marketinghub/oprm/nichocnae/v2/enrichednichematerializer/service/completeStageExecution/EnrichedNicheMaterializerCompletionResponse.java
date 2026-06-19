package com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.completeStageExecution;

/** Resposta de conclusão persistida da etapa enriched-niche-materializer. */
public record EnrichedNicheMaterializerCompletionResponse(
        String stageExecutionId,
        String status,
        String nextStageCode,
        String materializationDecision,
        String validationLevel,
        Double confidence,
        Long materializedNicheId) {}
