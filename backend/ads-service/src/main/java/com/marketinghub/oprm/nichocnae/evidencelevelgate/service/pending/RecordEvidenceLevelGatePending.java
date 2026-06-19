package com.marketinghub.oprm.nichocnae.evidencelevelgate.service.pending;

/** Representa um cartão aprovado na qualidade aguardando classificação comercial E0-E5 pelo executor externo. */
public record RecordEvidenceLevelGatePending(
    Long routineCardId,
    Long researchCycleId,
    String nicheName,
    String routineSummary,
    String painsSummary,
    String resultsSummary,
    String evidenceSummary,
    String sourceDomains,
    Integer confidenceScore,
    Integer routineEvidenceScore,
    Integer difficultyEvidenceScore,
    Integer sourceDiversityScore,
    Integer specificityScore,
    Integer qualityConfidenceScore) {}
