package com.marketinghub.oprm.nichocnae.sourcesearcher.service.detailStageExecution;

import com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution.SourceCandidateResponse;
import java.util.List;

/** Representa o detalhe das fontes candidatas encontradas para um ciclo de pesquisa. */
public record SourceSearcherDetailResponse(
    Long researchCycleId,
    String cycleStatus,
    Integer cycleTotalQueries,
    Integer cycleTotalSourceCandidates,
    List<SourceCandidateResponse> candidates) {}
