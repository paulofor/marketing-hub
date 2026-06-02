package com.marketinghub.nichocnae.routineresearchorchestrator;

import java.math.BigDecimal;
import java.time.Instant;

/** Representa o próximo nicho CNAE elegível retornado pelo backend para a etapa zero. */
public record RoutineResearchOrchestratorPending(
        Long sourceNicheId,
        String cnaeCode,
        String cnaeDescription,
        String nicheName,
        BigDecimal sourceScore,
        String routineResearchStatus,
        Long lastRoutineResearchCycleId,
        Instant createdAt) {}
