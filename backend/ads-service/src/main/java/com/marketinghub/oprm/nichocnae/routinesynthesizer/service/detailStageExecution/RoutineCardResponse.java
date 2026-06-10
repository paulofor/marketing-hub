package com.marketinghub.oprm.nichocnae.routinesynthesizer.service.detailStageExecution;

import java.time.Instant;

/** Cartão de rotina exposto para acompanhamento da etapa seis no frontend. */
public record RoutineCardResponse(
    Long routineCardId,
    Long researchCycleId,
    String nicheName,
    String routineSummary,
    String customerBehaviorSummary,
    String channelsSummary,
    String operationalPainsSummary,
    String emotionalPainsSummary,
    String dreamsSummary,
    String fearsSummary,
    String languageSummary,
    String painsSummary,
    String resultsSummary,
    String mechanismOpportunitiesSummary,
    String evidenceSummary,
    String sourceDomains,
    Integer confidenceScore,
    Integer routineEvidenceScore,
    Integer difficultyEvidenceScore,
    Integer sourceDiversityScore,
    Integer solutionLanguageRiskScore,
    String synthesizedBy,
    Instant createdAt) {}
