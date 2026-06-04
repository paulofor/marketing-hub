package com.marketinghub.oprm.nichocnae.routinesynthesizer.service.detailStageExecution;

/** Detalhe público da etapa seis para um ciclo de pesquisa de rotina. */
public record RoutineSynthesizerDetailResponse(
    Long researchCycleId,
    String cycleStatus,
    Integer cycleTotalExtractedSignals,
    RoutineCardResponse routineCard) {}
