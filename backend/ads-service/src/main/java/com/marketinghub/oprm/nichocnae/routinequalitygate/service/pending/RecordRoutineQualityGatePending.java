package com.marketinghub.oprm.nichocnae.routinequalitygate.service.pending;

import java.time.Instant;

/** Representa um cartão de rotina pendente de avaliação pela etapa sete do OPRM NichoCNAE. */
public record RecordRoutineQualityGatePending(
    Long routineCardId,
    Long researchCycleId,
    String nicheName,
    String routineSummary,
    String painsSummary,
    String resultsSummary,
    String mechanismOpportunitiesSummary,
    String evidenceSummary,
    String sourceDomains,
    Integer cardConfidenceScore,
    Integer sourceCount,
    Integer signalCount,
    Integer questionSignalCount,
    Integer painSignalCount,
    Integer mechanismOpportunityCount,
    Integer routineTaskCount,
    Integer commercialObjectCount,
    Instant createdAt) {}
