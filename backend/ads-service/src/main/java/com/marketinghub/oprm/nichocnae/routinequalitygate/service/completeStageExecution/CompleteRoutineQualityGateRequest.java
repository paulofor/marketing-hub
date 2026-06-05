package com.marketinghub.oprm.nichocnae.routinequalitygate.service.completeStageExecution;

/** Payload usado pelo coletor para concluir a etapa sete com a decisão de qualidade do cartão. */
public record CompleteRoutineQualityGateRequest(
    Long researchCycleId,
    Long routineCardId,
    String qualityStatus,
    Boolean readyForHypothesis,
    Integer specificityScore,
    Integer confidenceScore,
    Integer duplicationScore,
    String qualityNotes,
    String checkedBy) {}
