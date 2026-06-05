package com.marketinghub.nichocnae.sourcefetcher;

import java.time.Instant;

/** Representa uma fonte candidata pendente para coleta curta pela etapa quatro no coletor OPRM. */
public record SourceFetcherPending(
        Long sourceCandidateId,
        Long researchCycleId,
        Long researchQueryId,
        String sourceUrl,
        String sourceTitle,
        String sourceSnippet,
        String sourceDomain,
        String sourceGroup,
        String sourceIntent,
        Integer routineEvidenceScore,
        Boolean commercialPageRisk,
        Boolean solutionLanguageRisk,
        String searchProvider,
        Integer searchPosition,
        String status,
        Instant createdAt,
        Instant updatedAt) {}
