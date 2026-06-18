package com.marketinghub.oprm.nichocnae.routineresearchcycle.service.detailStageExecution;

import java.math.BigDecimal;
import java.time.Instant;

/** Detalha o estado operacional completo de uma execução da etapa de ciclo de pesquisa de rotina. */
public record RecordBackendRoutineResearchCycleDetalheDto(
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
    String currentStageCode,
    Integer totalQueries,
    Integer totalSourceCandidates,
    Integer totalSourceSnapshots,
    Integer totalExtractedSignals,
    Instant startedAt,
    Instant finishedAt,
    String errorMessage) {}
