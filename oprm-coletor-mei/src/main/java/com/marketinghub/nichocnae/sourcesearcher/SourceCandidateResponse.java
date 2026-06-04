package com.marketinghub.nichocnae.sourcesearcher;

import java.time.Instant;

/** Representa uma fonte candidata persistida pelo backend após conclusão da etapa três. */
public record SourceCandidateResponse(
        Long sourceCandidateId,
        Long researchCycleId,
        Long researchQueryId,
        String sourceUrl,
        String sourceTitle,
        String sourceSnippet,
        String sourceDomain,
        String sourceGroup,
        String searchProvider,
        Integer searchPosition,
        String status,
        Instant createdAt,
        Instant updatedAt) {}
