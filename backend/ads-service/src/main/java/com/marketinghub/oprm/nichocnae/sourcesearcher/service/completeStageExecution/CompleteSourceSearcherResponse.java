package com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution;

import java.util.List;

/** Representa o resultado da conclusão da etapa três para uma frase de pesquisa. */
public record CompleteSourceSearcherResponse(
    Long researchQueryId,
    Long researchCycleId,
    String queryText,
    String queryStatus,
    Integer resultCount,
    Integer cycleTotalSourceCandidates,
    List<SourceCandidateResponse> candidates) {}
