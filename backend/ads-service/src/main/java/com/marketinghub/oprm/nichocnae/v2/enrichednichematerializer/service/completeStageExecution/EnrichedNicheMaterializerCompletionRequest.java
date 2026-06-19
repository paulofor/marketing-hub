package com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.completeStageExecution;

/** Contrato de escrita para registrar a materialização decidida e executada pelo executor externo. */
public record EnrichedNicheMaterializerCompletionRequest(
        String materializationDecision,
        String validationLevel,
        Double confidence,
        Long materializedNicheId,
        String nextStageCode,
        String outputPayload) {}
