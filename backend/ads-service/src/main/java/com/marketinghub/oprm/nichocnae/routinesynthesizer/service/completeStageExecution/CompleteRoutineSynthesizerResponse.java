package com.marketinghub.oprm.nichocnae.routinesynthesizer.service.completeStageExecution;

import java.time.Instant;

/** Resposta de conclusão da etapa seis após persistir o cartão de rotina. */
public record CompleteRoutineSynthesizerResponse(
    Long routineCardId,
    Long researchCycleId,
    String cycleStatus,
    String nicheName,
    Integer confidenceScore,
    Instant createdAt) {}
