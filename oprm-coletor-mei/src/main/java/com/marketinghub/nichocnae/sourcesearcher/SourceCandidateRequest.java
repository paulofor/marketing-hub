package com.marketinghub.nichocnae.sourcesearcher;

import java.time.Instant;

/** Representa uma fonte candidata classificada no payload de conclusão da etapa três enviado ao backend. */
public record SourceCandidateRequest(
        String sourceUrl,
        String sourceTitle,
        String sourceSnippet,
        String sourceDomain,
        String sourceGroup,
        Integer searchPosition,
        String status,
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
        Instant publishedAt) {}
