package com.marketinghub.nichocnae.enrichednichematerializer;

import java.time.Instant;

/** Saída operacional da etapa final depois da persistência no backend. */
public record EnrichedNicheMaterializerOutput(
        Long researchCycleId,
        Long routineCardId,
        Long marketNicheId,
        Long enrichedNicheProfileId,
        String cycleStatus,
        Instant materializedAt) {}
