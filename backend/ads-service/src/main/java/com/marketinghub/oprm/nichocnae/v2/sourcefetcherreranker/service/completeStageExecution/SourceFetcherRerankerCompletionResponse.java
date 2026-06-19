package com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.completeStageExecution;

/** Contrato devolvido após o backend registrar a conclusão source-fetcher-reranker do NichoCNAE v2. */
public record SourceFetcherRerankerCompletionResponse(
        String stageExecutionId,
        String status,
        String nextStageCode,
        String sourceFetchDecision,
        Integer fetchedSnapshotCount,
        Integer selectedSourceCount,
        Integer rejectedSourceCount) {}
