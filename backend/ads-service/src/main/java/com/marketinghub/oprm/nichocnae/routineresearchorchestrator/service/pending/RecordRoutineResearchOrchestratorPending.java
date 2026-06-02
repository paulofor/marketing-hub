package com.marketinghub.oprm.nichocnae.routineresearchorchestrator.service.pending;

import java.math.BigDecimal;
import java.time.Instant;

/** Representa um nicho CNAE elegível para a etapa zero iniciar a pesquisa de rotina. */
public record RecordRoutineResearchOrchestratorPending(
    Long sourceNicheId,
    String cnaeCode,
    String cnaeDescription,
    String nicheName,
    BigDecimal sourceScore,
    String routineResearchStatus,
    Long lastRoutineResearchCycleId,
    Instant createdAt) {}
