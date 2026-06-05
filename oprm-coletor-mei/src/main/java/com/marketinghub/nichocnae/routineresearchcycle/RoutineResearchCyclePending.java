package com.marketinghub.nichocnae.routineresearchcycle;

import java.math.BigDecimal;
import java.time.Instant;

/** Representa uma unidade de trabalho da etapa um criada pelo backend para controlar o ciclo de rotina. */
public record RoutineResearchCyclePending(
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        String cnaeDescription,
        String nicheName,
        String originalNicheName,
        String neutralNicheName,
        String researchMode,
        BigDecimal solutionLanguageRiskScore,
        BigDecimal sourceScore,
        String triggerSource,
        String status,
        Instant startedAt,
        Instant createdAt) {}
