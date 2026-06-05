package com.marketinghub.nichocnae.routineresearchcycle;

import java.math.BigDecimal;
import java.time.Instant;

/** Detalha uma execução completa da etapa um de ciclo de pesquisa de rotina do nicho CNAE. */
public record RoutineResearchCycleDetail(
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
        String status,
        Integer totalQueries,
        Integer totalSourceCandidates,
        Integer totalSourceSnapshots,
        Integer totalExtractedSignals,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage) {}
