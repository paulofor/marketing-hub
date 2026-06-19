package com.marketinghub.oprm.nichocnae.v2.sourcefetcherreranker.service.completeStageExecution;

/** Contrato recebido do executor ao concluir a etapa source-fetcher-reranker do NichoCNAE v2. */
public record SourceFetcherRerankerCompletionRequest(
        String sourceFetchDecision,
        Integer fetchedSnapshotCount,
        Integer selectedSourceCount,
        Integer rejectedSourceCount,
        String outputPayload,
        String nextStageCode) {}
