package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.recent;

import java.math.BigDecimal;
import java.time.Instant;

/** Representa um nicho recentemente processado pela etapa zero do pipeline OPRM nichocnae. */
public record RecordRoutineResearchOrchestratorRecent(
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        String cnaeDescription,
        String nicheName,
        BigDecimal sourceScore,
        String triggerSource,
        String cycleStatus,
        Instant processedAt) {}
