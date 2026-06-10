package com.marketinghub.nichocnae.routinesynthesizer;

/** Rascunho funcional do cartão de rotina antes da persistência no backend. */
public record RoutineCardDraft(
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
        Integer solutionLanguageRiskScore) {}
