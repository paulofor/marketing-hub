package com.marketinghub.nichocnae.routinesynthesizer;

import java.time.Instant;

/** Resposta do backend após persistir o cartão de rotina da etapa seis. */
public record RoutineSynthesizerCompletionResponse(
        Long routineCardId,
        Long researchCycleId,
        String cycleStatus,
        String nicheName,
        Integer confidenceScore,
        Instant createdAt) {}
