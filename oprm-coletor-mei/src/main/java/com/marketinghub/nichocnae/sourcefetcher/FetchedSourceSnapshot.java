package com.marketinghub.nichocnae.sourcefetcher;

/** Representa os metadados e o trecho curto extraídos de uma fonte pública durante a etapa quatro. */
public record FetchedSourceSnapshot(
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
