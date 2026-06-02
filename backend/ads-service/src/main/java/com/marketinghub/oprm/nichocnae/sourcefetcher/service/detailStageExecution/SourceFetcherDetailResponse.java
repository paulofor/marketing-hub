package com.marketinghub.oprm.nichocnae.sourcefetcher.service.detailStageExecution;

import com.marketinghub.oprm.nichocnae.sourcefetcher.service.completeStageExecution.SourceSnapshotResponse;
import java.util.List;

/** Representa os snapshots coletados para um ciclo de pesquisa de rotina. */
public record SourceFetcherDetailResponse(
    Long researchCycleId,
    String cycleStatus,
    Integer cycleTotalSourceCandidates,
    Integer cycleTotalSourceSnapshots,
    List<SourceSnapshotResponse> snapshots) {}
