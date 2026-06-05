package com.marketinghub.oprm.nichocnae.sourcefetcher.service.completeStageExecution;

/** Representa o payload de conclusão da etapa quatro com metadados, classificação e trecho curto coletado. */
public record CompleteSourceFetcherRequest(
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
    Integer relevanceScore) {}
