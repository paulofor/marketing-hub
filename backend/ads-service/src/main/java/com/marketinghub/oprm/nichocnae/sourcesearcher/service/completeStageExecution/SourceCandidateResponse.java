package com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution;

import java.time.Instant;

/** Representa uma fonte candidata persistida pela etapa três de busca de fontes. */
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
