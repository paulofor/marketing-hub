package com.marketinghub.nichocnae.routinesynthesizer;

/** Payload enviado ao backend para concluir a etapa seis. */
public record RoutineSynthesizerCompletionRequest(
        Long researchCycleId,
        String nicheName,
        String routineSummary,
        String painsSummary,
        String resultsSummary,
        String mechanismOpportunitiesSummary,
        String evidenceSummary,
        String sourceDomains,
        Integer confidenceScore,
        String synthesizedBy) {}
