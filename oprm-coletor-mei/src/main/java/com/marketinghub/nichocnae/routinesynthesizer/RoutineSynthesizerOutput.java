package com.marketinghub.nichocnae.routinesynthesizer;

import java.time.Instant;

/** Saída operacional da etapa seis após criação do cartão de rotina. */
public record RoutineSynthesizerOutput(
        Long routineCardId,
        Long researchCycleId,
        String cycleStatus,
        String nicheName,
        Integer confidenceScore,
        Instant createdAt) {}
