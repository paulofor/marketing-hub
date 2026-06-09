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
    String sourceIntent,
    Integer routineEvidenceScore,
    Boolean commercialPageRisk,
    Boolean solutionLanguageRisk,
    String sourceClassificationType,
    Integer sourceFreshnessScore,
    Boolean outdatedSourceRisk,
    Integer brazilRelevanceScore,
    Integer autonomousProfessionalEvidenceScore,
    Boolean structuredBusinessDriftRisk,
    Instant publishedAt,
    Instant createdAt,
    Instant updatedAt) {}
