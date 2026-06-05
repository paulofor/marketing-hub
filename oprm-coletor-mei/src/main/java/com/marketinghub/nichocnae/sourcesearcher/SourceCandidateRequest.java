package com.marketinghub.nichocnae.sourcesearcher;

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
        Boolean solutionLanguageRisk) {}
