package com.marketinghub.nichocnae.routineresearchcycle;

import java.math.BigDecimal;
import java.time.Instant;

/** Resume um ciclo de pesquisa de rotina para listagem operacional no coletor OPRM. */
public record RoutineResearchCycleSummary(
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        String nicheName,
        String originalNicheName,
        String neutralNicheName,
        String researchMode,
        BigDecimal solutionLanguageRiskScore,
        BigDecimal sourceScore,
        String status,
        Integer totalQueries,
        Integer totalExtractedSignals,
        Instant startedAt,
        Instant finishedAt) {}
