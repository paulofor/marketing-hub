package com.marketinghub.oprm.nichocnae.routinequalitygate.service.completeStageExecution;

import java.time.Instant;

/** Resposta retornada após persistir a decisão da etapa sete no cartão de rotina. */
public record CompleteRoutineQualityGateResponse(
    Long routineCardId,
    Long researchCycleId,
    String cycleStatus,
    String qualityStatus,
    Boolean readyForHypothesis,
    Integer specificityScore,
    Integer confidenceScore,
    Integer duplicationScore,
    Instant checkedAt) {}
