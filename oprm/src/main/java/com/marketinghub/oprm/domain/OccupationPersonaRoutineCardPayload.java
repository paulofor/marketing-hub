package com.marketinghub.oprm.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OccupationPersonaRoutineCardPayload(
        String personaLabel,
        String occupationName,
        List<String> occupationAliases,
        String nicheName,
        String routineSummary,
        List<RoutineTaskPattern> topTasks,
        List<String> topTools,
        List<RoutineConstraintSignal> topConstraints,
        List<String> topWorkContexts,
        String customerInteractionPattern,
        String revenueDependencyPattern,
        String adminBurdenPattern,
        List<RoutineWorkaroundSignal> workaroundPatterns,
        List<RoutinePainSignal> painSignals,
        List<String> desiredOutcomeSignals,
        List<String> mechanismOpportunitySignals,
        List<String> evidenceRefs,
        Map<String, Integer> sourceMix,
        Instant generatedAt) {
}
