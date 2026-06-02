package com.marketinghub.oprm.nichocnae.sourcefetcher.service.completeStageExecution;

import java.time.Instant;

/** Representa um snapshot curto de fonte persistido pela etapa quatro. */
public record SourceSnapshotResponse(
    Long sourceSnapshotId,
    Long researchCycleId,
    Long sourceCandidateId,
    String sourceUrl,
    String sourceDomain,
    String sourceTitle,
    String sourceType,
    String snippet,
    String shortExcerpt,
    Instant fetchedAt,
    String fetchStatus,
    Integer httpStatus,
    String storagePolicy,
    String licenseState,
    String errorMessage,
    Instant createdAt) {}
