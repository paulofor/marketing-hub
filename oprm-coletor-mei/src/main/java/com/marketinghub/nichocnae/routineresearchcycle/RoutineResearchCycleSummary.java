package com.marketinghub.nichocnae.routineresearchcycle;

import java.math.BigDecimal;
import java.time.Instant;

/** Resume uma execução da etapa um para acompanhamento operacional por nicho CNAE de origem. */
public record RoutineResearchCycleSummary(
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        String nicheName,
        BigDecimal sourceScore,
        String status,
        Integer totalQueries,
        Integer totalExtractedSignals,
        Instant startedAt,
        Instant finishedAt) {}
