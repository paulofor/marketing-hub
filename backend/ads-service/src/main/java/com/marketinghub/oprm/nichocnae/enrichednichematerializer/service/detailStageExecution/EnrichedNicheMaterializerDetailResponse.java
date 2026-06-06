package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.detailStageExecution;

import java.math.BigDecimal;
import java.time.Instant;

/** Detalhe público da materialização de nicho enriquecido para a tela do pipeline. */
public record EnrichedNicheMaterializerDetailResponse(
    Long researchCycleId,
    String cycleStatus,
    Long routineCardId,
    Long marketNicheId,
    Long enrichedNicheProfileId,
    String originalNicheName,
    String neutralNicheName,
    String researchMode,
    BigDecimal solutionLanguageRiskScore,
    String nicheName,
    String cnaeCode,
    String qualityStatus,
    String routineSummary,
    String painsSummary,
    String resultsSummary,
    String mechanismOpportunitiesSummary,
    String evidenceSummary,
    String sourceDomains,
    Integer routineEvidenceScore,
    Integer difficultyEvidenceScore,
    Integer sourceDiversityScore,
    Integer materializedSolutionLanguageRiskScore,
    Instant materializedAt) {}
