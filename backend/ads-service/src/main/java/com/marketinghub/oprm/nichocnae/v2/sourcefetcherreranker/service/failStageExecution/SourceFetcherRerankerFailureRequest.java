package com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.failStageExecution;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2FailureType;

/** Contrato recebido do executor para registrar falha da etapa source-fetcher-reranker do NichoCNAE v2. */
public record SourceFetcherRerankerFailureRequest(
        OprmNichoCnaeV2FailureType failureType, String errorMessage, String inputPayload) {}
