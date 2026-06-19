package com.marketinghub.oprm.nichocnae.v2.enrichednichematerializer.service.failStageExecution;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;

/** Contrato de escrita para registrar falha técnica ou cognitiva da etapa enriched-niche-materializer. */
public record EnrichedNicheMaterializerFailureRequest(
        OprmNichoCnaeV2FailureType failureType,
        String errorMessage,
        String inputPayload) {}
