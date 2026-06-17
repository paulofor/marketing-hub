package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.reprocess;

/** Representa o resultado da reabertura de um job CNAE para reexecução de etapas. */
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
