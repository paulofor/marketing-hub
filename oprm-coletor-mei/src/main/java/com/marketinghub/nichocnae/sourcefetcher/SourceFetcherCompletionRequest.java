package com.marketinghub.nichocnae.sourcefetcher;

import java.time.Instant;

/** Representa o payload enviado ao backend para concluir a coleta curta de uma fonte candidata. */
public record SourceFetcherCompletionRequest(
        String sourceUrl,
        String sourceDomain,
        String sourceTitle,
        String sourceType,
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
        String snippet,
        String shortExcerpt,
        String fetchStatus,
        Integer httpStatus,
        String storagePolicy,
        String licenseState,
        Integer relevanceScore) {}
