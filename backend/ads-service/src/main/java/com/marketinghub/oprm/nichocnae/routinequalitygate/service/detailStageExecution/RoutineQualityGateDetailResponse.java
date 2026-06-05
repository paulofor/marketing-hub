package com.marketinghub.oprm.nichocnae.routinequalitygate.service.detailStageExecution;

import java.time.Instant;

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
    String qualityNotes,
    String checkedBy,
    Instant checkedAt) {}
