package com.marketinghub.oprm.nichocnae.signalextractor.service.pending;

import java.time.Instant;

/** Representa um snapshot curto pendente para extração estruturada de sinais na etapa cinco. */
public record RecordSignalExtractorPending(
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
