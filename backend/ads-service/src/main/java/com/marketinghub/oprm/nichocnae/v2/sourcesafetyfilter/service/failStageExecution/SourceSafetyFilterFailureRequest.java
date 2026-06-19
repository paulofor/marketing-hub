package com.marketinghub.oprm.nichocnae.v2.sourcesafetyfilter.service.failStageExecution;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;

/** Contrato recebido do executor ao reportar falha da etapa source-safety-filter do NichoCNAE v2. */
public record SourceSafetyFilterFailureRequest(
        OprmNichoCnaeV2FailureType failureType,
        String errorCode,
        String errorMessage,
        String inputPayload) {}
