package com.marketinghub.oprm.nichocnae.sourcefetcher.service.completeStageExecution;

import java.time.Instant;

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
  /** Mantém compatibilidade para snapshots curtos sem os novos indicadores de fonte. */
  public CompleteSourceFetcherRequest(
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
