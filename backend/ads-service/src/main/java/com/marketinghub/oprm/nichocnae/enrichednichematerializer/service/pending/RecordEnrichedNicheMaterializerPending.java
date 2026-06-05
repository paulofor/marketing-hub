package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.pending;

import java.math.BigDecimal;
import java.time.Instant;

/** Unidade de trabalho fechada para a etapa final que materializa nicho e nicho enriquecido. */
public record RecordEnrichedNicheMaterializerPending(
    Long routineCardId,
    Long researchCycleId,
    Long sourceNicheCandidateId,
    Long existingMarketNicheId,
    String cnaeCode,
    String cnaeDescription,
    String nicheName,
    BigDecimal sourceScore,
    String qualityStatus,
    Integer specificityScore,
    Integer confidenceScore,
    Integer duplicationScore,
    String routineSummary,
    String painsSummary,
    String resultsSummary,
    String mechanismOpportunitiesSummary,
    String evidenceSummary,
    String sourceDomains,
    Instant qualityCheckedAt) {}
