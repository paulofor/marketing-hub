package com.marketinghub.nichocnae.sourcesearcher;

import java.util.List;

/** Representa a saída interna da etapa três depois que o backend grava as fontes candidatas. */
public record SourceSearcherOutput(
        Long researchQueryId,
        Long researchCycleId,
        String queryText,
        String queryStatus,
        Integer resultCount,
        Integer cycleTotalSourceCandidates,
        List<SourceCandidateResponse> candidates) {}
