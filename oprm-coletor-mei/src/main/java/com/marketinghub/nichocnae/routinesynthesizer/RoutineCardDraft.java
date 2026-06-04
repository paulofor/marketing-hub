package com.marketinghub.nichocnae.routinesynthesizer;

/** Rascunho funcional do cartão de rotina antes da persistência no backend. */
public record RoutineCardDraft(
        String routineSummary,
        String painsSummary,
        String resultsSummary,
        String mechanismOpportunitiesSummary,
        String evidenceSummary,
        String sourceDomains,
        Integer confidenceScore) {}
