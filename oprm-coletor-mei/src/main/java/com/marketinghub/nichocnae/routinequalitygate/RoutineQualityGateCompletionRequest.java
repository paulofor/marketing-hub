package com.marketinghub.nichocnae.routinequalitygate;

/** Payload enviado pelo coletor ao backend para concluir a etapa sete. */
public record RoutineQualityGateCompletionRequest(
        Long researchCycleId,
        Long routineCardId,
        String qualityStatus,
        Boolean readyForHypothesis,
        Integer specificityScore,
        Integer confidenceScore,
        Integer duplicationScore,
        String qualityNotes,
        String checkedBy) {}
