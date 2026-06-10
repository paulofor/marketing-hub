package com.marketinghub.nichocnae.routinesynthesizer;

/** Payload enviado ao backend para concluir a etapa seis. */
public record RoutineSynthesizerCompletionRequest(
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
