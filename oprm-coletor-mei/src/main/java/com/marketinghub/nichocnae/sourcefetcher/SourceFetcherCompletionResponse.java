package com.marketinghub.nichocnae.sourcefetcher;

/** Representa a resposta do backend após concluir a etapa quatro para uma fonte candidata. */
public record SourceFetcherCompletionResponse(
        Long sourceCandidateId,
        Long researchCycleId,
        Boolean selectedForFetch,
        Integer relevanceScore,
        Integer cycleTotalSourceSnapshots,
        SourceSnapshotResponse snapshot) {}
