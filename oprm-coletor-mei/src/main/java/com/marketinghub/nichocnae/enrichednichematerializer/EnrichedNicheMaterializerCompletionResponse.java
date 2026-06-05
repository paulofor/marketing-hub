package com.marketinghub.nichocnae.enrichednichematerializer;

import java.time.Instant;

/** Resposta do backend após alimentar nicho e nicho enriquecido. */
public record EnrichedNicheMaterializerCompletionResponse(
        Long researchCycleId,
        Long routineCardId,
        Long marketNicheId,
        Long enrichedNicheProfileId,
        String cycleStatus,
        Instant materializedAt) {}
