package com.marketinghub.oprm.nichocnae.routineresearchcycle.service.listStageExecutions;

import java.math.BigDecimal;
import java.time.Instant;

/** Resume uma execução do ciclo de pesquisa de rotina para listagem operacional no backend OPRM. */
public record RoutineResearchCycleExecutionSummaryResponse(
    Long researchCycleId,
    Long sourceNicheId,
    String cnaeCode,
    String nicheName,
    String originalNicheName,
    String neutralNicheName,
    String researchMode,
    BigDecimal solutionLanguageRiskScore,
    BigDecimal sourceScore,
    String triggerSource,
    String status,
    String currentStageCode,
    Integer totalQueries,
    Integer totalSourceCandidates,
    Integer totalSourceSnapshots,
    Integer totalExtractedSignals,
    BigDecimal executionCostUsd,
    BigDecimal cnaeTotalCostUsd,
    Instant startedAt,
    Instant finishedAt,
    String errorMessage) {}
