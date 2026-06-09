package com.marketinghub.nichocnae.sourcefetcher;

import java.time.Instant;

/** Representa os metadados e o trecho curto extraídos de uma fonte pública durante a etapa quatro. */
public record FetchedSourceSnapshot(
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
        Integer relevanceScore) {
    /** Mantém compatibilidade para chamadas que ainda propagam apenas indicadores de rotina legados. */
    public FetchedSourceSnapshot(
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
            String fetchStatus,
            Integer httpStatus,
            String storagePolicy,
            String licenseState,
            Integer relevanceScore) {
        this(
                sourceUrl,
                sourceDomain,
                sourceTitle,
                sourceType,
                sourceIntent,
                routineEvidenceScore,
                commercialPageRisk,
                solutionLanguageRisk,
                null,
                null,
                false,
                null,
                null,
                false,
                null,
                snippet,
                shortExcerpt,
                fetchStatus,
                httpStatus,
                storagePolicy,
                licenseState,
                relevanceScore);
    }
}
