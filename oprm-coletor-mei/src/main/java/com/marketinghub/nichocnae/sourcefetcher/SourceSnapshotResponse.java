package com.marketinghub.nichocnae.sourcefetcher;

import java.time.Instant;

/** Representa o snapshot curto persistido pelo backend para uma fonte coletada na etapa quatro. */
public record SourceSnapshotResponse(
        Long sourceSnapshotId,
        Long researchCycleId,
        Long sourceCandidateId,
        String sourceUrl,
        String sourceDomain,
        String sourceTitle,
        String sourceType,
        String sourceIntent,
        Integer routineEvidenceScore,
        Boolean commercialPageRisk,
        Boolean solutionLanguageRisk,
        String snippet,
        String shortExcerpt,
        Instant fetchedAt,
        String fetchStatus,
        Integer httpStatus,
        String storagePolicy,
        String licenseState,
        String errorMessage,
        Instant createdAt) {}
