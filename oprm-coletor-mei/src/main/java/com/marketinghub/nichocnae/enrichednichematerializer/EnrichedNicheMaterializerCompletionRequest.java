package com.marketinghub.nichocnae.enrichednichematerializer;

/** Payload enviado pelo coletor ao backend para concluir a etapa final de materialização. */
public record EnrichedNicheMaterializerCompletionRequest(
        Long researchCycleId,
        Long routineCardId,
        String personaSummary,
        String languagePatterns,
        String commercialTriggers,
        String objections,
        String materializedBy) {}
