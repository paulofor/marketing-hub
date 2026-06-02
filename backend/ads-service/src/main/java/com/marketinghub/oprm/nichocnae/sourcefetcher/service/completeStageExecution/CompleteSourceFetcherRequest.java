package com.marketinghub.oprm.nichocnae.sourcefetcher.service.completeStageExecution;

/** Representa o payload de conclusão da etapa quatro com metadados e trecho curto coletado. */
public record CompleteSourceFetcherRequest(
    String sourceUrl,
    String sourceDomain,
    String sourceTitle,
    String sourceType,
    String snippet,
    String shortExcerpt,
    String fetchStatus,
    Integer httpStatus,
    String storagePolicy,
    String licenseState,
    Integer relevanceScore) {}
