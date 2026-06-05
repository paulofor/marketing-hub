package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution;

/** Payload usado pelo coletor para concluir a materialização final do nicho enriquecido. */
public record CompleteEnrichedNicheMaterializerRequest(
    Long researchCycleId,
    Long routineCardId,
    String personaSummary,
    String languagePatterns,
    String commercialTriggers,
    String objections,
    String materializedBy) {}
