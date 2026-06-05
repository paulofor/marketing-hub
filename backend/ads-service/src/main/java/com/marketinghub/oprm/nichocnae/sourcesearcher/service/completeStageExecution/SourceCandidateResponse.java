package com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution;

import java.time.Instant;

/** Representa uma fonte candidata persistida com escore e marcação de risco da etapa três de busca de fontes. */
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
    Integer relevanceScore,
    String rejectionReason,
    Instant createdAt,
    Instant updatedAt) {}
