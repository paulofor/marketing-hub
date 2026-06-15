package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.generatedByCnae;

import java.time.Instant;

/** Resumo público de um nicho enriquecido já gerado para um CNAE. */
public record GeneratedEnrichedNicheByCnaeResponse(
    Long enrichedNicheProfileId,
    Long marketNicheId,
    Long researchCycleId,
    String cnaeCode,
    String cnaeDescription,
    String nicheName,
    String qualityStatus,
    Integer routineEvidenceScore,
    Integer difficultyEvidenceScore,
    Integer sourceDiversityScore,
    Instant materializedAt) {}
