package com.marketinghub.nichocnae.routinequalitygate;

/** Representa a decisão determinística da etapa sete antes de envio ao backend. */
public record RoutineQualityDecision(
        String qualityStatus,
        Boolean readyForHypothesis,
        Integer specificityScore,
        Integer confidenceScore,
        Integer duplicationScore,
        String qualityNotes) {}
