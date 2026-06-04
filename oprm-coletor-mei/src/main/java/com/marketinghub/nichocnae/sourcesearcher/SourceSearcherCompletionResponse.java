package com.marketinghub.nichocnae.sourcesearcher;

import java.util.List;

/** Representa a resposta do backend após persistir resultados de uma query da etapa três. */
public record SourceSearcherCompletionResponse(
        Long researchQueryId,
        Long researchCycleId,
        String queryText,
        String queryStatus,
        Integer resultCount,
        Integer cycleTotalSourceCandidates,
        List<SourceCandidateResponse> candidates) {}
