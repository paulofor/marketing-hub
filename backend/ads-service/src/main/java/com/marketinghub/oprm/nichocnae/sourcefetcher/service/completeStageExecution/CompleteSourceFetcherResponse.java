package com.marketinghub.oprm.nichocnae.sourcefetcher.service.completeStageExecution;

/** Representa o resultado da conclusão da etapa quatro para uma fonte candidata. */
public record CompleteSourceFetcherResponse(
    Long sourceCandidateId,
    Long researchCycleId,
    Boolean selectedForFetch,
    Integer relevanceScore,
    Integer cycleTotalSourceSnapshots,
    SourceSnapshotResponse snapshot) {}
