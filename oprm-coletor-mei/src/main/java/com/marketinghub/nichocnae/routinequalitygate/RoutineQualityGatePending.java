package com.marketinghub.nichocnae.routinequalitygate;

import java.time.Instant;

/** Representa o insumo de avaliação de qualidade recebido do backend para a etapa sete. */
public record RoutineQualityGatePending(
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
