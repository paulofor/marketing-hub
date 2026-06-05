package com.marketinghub.nichocnae.routinequalitygate;

import java.time.Instant;

/** Saída operacional da etapa sete depois que o backend persiste a decisão de qualidade. */
public record RoutineQualityGateOutput(
        Long routineCardId,
        Long researchCycleId,
        String cycleStatus,
        String qualityStatus,
        Boolean readyForHypothesis,
        Integer specificityScore,
        Integer confidenceScore,
        Integer duplicationScore,
        Instant checkedAt) {}
