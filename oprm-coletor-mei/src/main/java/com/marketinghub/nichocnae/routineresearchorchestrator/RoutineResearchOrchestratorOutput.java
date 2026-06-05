package com.marketinghub.nichocnae.routineresearchorchestrator;

import java.math.BigDecimal;

/** Informa o resultado da etapa zero executada contra o backend OPRM nichocnae. */
public record RoutineResearchOrchestratorOutput(
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
