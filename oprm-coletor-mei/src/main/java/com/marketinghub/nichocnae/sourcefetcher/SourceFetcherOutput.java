package com.marketinghub.nichocnae.sourcefetcher;

/** Representa a saída interna da etapa quatro após o backend gravar o snapshot curto da fonte. */
public record SourceFetcherOutput(
        Long sourceCandidateId,
        Long researchCycleId,
        Boolean selectedForFetch,
        Integer relevanceScore,
        Integer cycleTotalSourceSnapshots,
        SourceSnapshotResponse snapshot) {}
