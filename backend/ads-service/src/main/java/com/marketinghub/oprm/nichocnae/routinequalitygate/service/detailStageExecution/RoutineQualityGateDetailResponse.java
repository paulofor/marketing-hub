package com.marketinghub.oprm.nichocnae.routinequalitygate.service.detailStageExecution;

import java.time.Instant;
import java.util.Map;

/** Detalhe público da avaliação de qualidade de um cartão de rotina OPRM NichoCNAE. */
public record RoutineQualityGateDetailResponse(
    Long researchCycleId,
    String cycleStatus,
    Long routineCardId,
    String qualityStatus,
    Boolean readyForHypothesis,
    Integer specificityScore,
    Integer confidenceScore,
    Integer duplicationScore,
    Integer routineEvidenceScore,
    Integer difficultyEvidenceScore,
    Integer sourceDiversityScore,
    Integer solutionLanguageRiskScore,
    Map<String, Object> qualityNotes,
    String checkedBy,
    Instant checkedAt) {}
