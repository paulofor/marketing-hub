package com.marketinghub.nichocnae.signalextractor;

import java.time.Instant;

/** Representa um snapshot curto pendente para extração de sinais pela etapa cinco no coletor. */
public record SignalExtractorPending(
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
        String signalExtractionStatus,
        Instant createdAt) {}
