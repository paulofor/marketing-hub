package com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.failStageExecution;

/** Contrato devolvido após o backend registrar falha source-fetcher-reranker do NichoCNAE v2. */
public record SourceFetcherRerankerFailureResponse(
        String stageExecutionId,
        String status,
        String retryStageExecutionId,
        Integer attemptNumber,
        Integer technicalRetryNumber) {}
