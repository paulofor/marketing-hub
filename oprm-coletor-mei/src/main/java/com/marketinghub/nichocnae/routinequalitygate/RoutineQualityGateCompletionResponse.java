package com.marketinghub.nichocnae.routinequalitygate;

import java.time.Instant;

/** Resposta do backend após conclusão da etapa sete. */
public record RoutineQualityGateCompletionResponse(
        Long routineCardId,
        Long researchCycleId,
        String cycleStatus,
        String qualityStatus,
        Boolean readyForHypothesis,
        Integer specificityScore,
        Integer confidenceScore,
        Integer duplicationScore,
        Instant checkedAt) {}
