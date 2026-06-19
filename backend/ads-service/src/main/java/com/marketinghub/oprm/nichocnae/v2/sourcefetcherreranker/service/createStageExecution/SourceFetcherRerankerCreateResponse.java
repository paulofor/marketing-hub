package com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.createStageExecution;

/** Contrato devolvido após o backend gravar a pendência source-fetcher-reranker do NichoCNAE v2. */
public record SourceFetcherRerankerCreateResponse(String stageExecutionId, String status, String stageCode) {}
