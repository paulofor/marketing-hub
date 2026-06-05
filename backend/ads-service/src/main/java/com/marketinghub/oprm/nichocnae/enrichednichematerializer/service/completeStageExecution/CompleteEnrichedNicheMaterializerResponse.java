package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution;

import java.time.Instant;

/** Resposta da etapa final depois de alimentar as tabelas de nicho e nicho enriquecido. */
public record CompleteEnrichedNicheMaterializerResponse(
    Long researchCycleId,
    Long routineCardId,
    Long marketNicheId,
    Long enrichedNicheProfileId,
    String cycleStatus,
    Instant materializedAt) {}
