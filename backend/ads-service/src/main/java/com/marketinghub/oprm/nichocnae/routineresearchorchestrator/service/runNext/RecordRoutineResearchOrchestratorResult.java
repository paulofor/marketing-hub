package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.runNext;

import java.math.BigDecimal;

/** Informa o resultado da execução da etapa zero do orquestrador de pesquisa de rotina. */
public record RecordRoutineResearchOrchestratorResult(
    boolean started,
    Long researchCycleId,
    Long sourceNicheId,
    String cnaeCode,
    String cnaeDescription,
    String nicheName,
    BigDecimal sourceScore,
    String triggerSource,
    String cycleStatus,
    String originalNicheName,
    String neutralNicheName,
    String researchMode,
    BigDecimal solutionLanguageRiskScore,
    String routineResearchStatus,
    String message) {}
