package com.marketinghub.oprm.nichocnae.sourcesearcher.service.detailStageExecution;

import com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution.SourceCandidateResponse;
import java.time.Instant;
import java.util.List;

/** Representa o detalhe e resumo da execução da etapa três para um ciclo de pesquisa. */
public record SourceSearcherDetailResponse(
    Long researchCycleId,
    String cycleStatus,
    Integer cycleTotalQueries,
    Integer cycleTotalSourceCandidates,
    Long pendingQueries,
    Long completedQueries,
    Long failedQueries,
    Instant lastExecutedAt,
    String lastSearchProvider,
    String lastErrorMessage,
    List<SourceCandidateResponse> candidates) {}
