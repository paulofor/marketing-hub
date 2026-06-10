package com.marketinghub.oprm.nichocnae.routinesynthesizer.service.completeStageExecution;

/** Payload de conclusão da etapa seis com o cartão de rotina sintetizado. */
public record CompleteRoutineSynthesizerRequest(
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
    String synthesizedBy) {}
