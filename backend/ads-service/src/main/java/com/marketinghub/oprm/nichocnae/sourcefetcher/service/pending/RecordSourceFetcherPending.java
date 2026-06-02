package com.marketinghub.oprm.nichocnae.sourcefetcher.service.pending;

import java.time.Instant;

/** Representa uma fonte candidata pendente para coleta curta pela etapa quatro. */
public record RecordSourceFetcherPending(
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
