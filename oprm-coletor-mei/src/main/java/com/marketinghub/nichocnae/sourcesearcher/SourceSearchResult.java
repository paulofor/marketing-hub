package com.marketinghub.nichocnae.sourcesearcher;

import java.time.Instant;

/** Representa um resultado público normalizado e classificado antes de envio ao backend da etapa três. */
public record SourceSearchResult(
        String sourceUrl,
        String sourceTitle,
        String sourceSnippet,
        String sourceDomain,
        Integer searchPosition,
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
        Instant publishedAt) {
    /** Mantém compatibilidade para testes e provedores que ainda não informam indicadores MEI/autônomo. */
    public SourceSearchResult(
            String sourceUrl,
            String sourceTitle,
            String sourceSnippet,
            String sourceDomain,
            Integer searchPosition,
            String sourceIntent,
            Integer routineEvidenceScore,
            Boolean commercialPageRisk,
            Boolean solutionLanguageRisk) {
        this(
                sourceUrl,
                sourceTitle,
                sourceSnippet,
                sourceDomain,
                searchPosition,
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
                null);
    }
}
