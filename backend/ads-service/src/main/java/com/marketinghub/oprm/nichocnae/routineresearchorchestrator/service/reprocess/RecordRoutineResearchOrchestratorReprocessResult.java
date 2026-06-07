package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.reprocess;

/** Representa o resultado da liberação manual de um nicho CNAE para novo ciclo automático. */
public record RecordRoutineResearchOrchestratorReprocessResult(
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        String cnaeDescription,
        String previousCycleStatus,
        String previousRoutineResearchStatus,
        String routineResearchStatus,
        Long lastRoutineResearchCycleId,
        String message) {}
