package com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution;

import java.time.Instant;

/** Representa um resultado classificado retornado pelo provedor de busca para ser salvo como fonte candidata. */
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
    Instant publishedAt) {
  /** Mantém compatibilidade para chamadas legadas que ainda não informam atualidade e aderência MEI/autônomo. */
  public SourceCandidateRequest(
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
      Boolean solutionLanguageRisk) {
    this(
        sourceUrl,
        sourceTitle,
        sourceSnippet,
        sourceDomain,
        sourceGroup,
        searchPosition,
        status,
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
