package com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.pending;

/** Contrato de leitura de pendência da etapa enriched-niche-materializer para o executor OPRM. */
public record EnrichedNicheMaterializerPendingResponse(
        String stageExecutionId,
        String jobId,
        String cnaeCode,
        Long sourceNicheId,
        Integer attemptNumber,
        Integer technicalRetryNumber,
        Integer knowledgeVersion,
        String inputPayload) {}
