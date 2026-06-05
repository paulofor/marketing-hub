package com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution;

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
    Boolean solutionLanguageRisk) {}
