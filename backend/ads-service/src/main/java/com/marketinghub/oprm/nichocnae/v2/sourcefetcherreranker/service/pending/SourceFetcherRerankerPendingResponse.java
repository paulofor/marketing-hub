package com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.pending;

/** Contrato entregue ao executor com a pendência da etapa source-fetcher-reranker do NichoCNAE v2. */
public record SourceFetcherRerankerPendingResponse(
        String stageExecutionId,
        String jobId,
        String cnaeCode,
        Long sourceNicheId,
        Integer attemptNumber,
        Integer technicalRetryNumber,
        Integer knowledgeVersion,
        String inputPayload) {}
