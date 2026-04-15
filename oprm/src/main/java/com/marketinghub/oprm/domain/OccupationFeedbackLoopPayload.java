package com.marketinghub.oprm.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OccupationFeedbackLoopPayload(
        String personaLabel,
        String occupationName,
        String nicheName,
        String baselineRoutineArtifactId,
        String baselineFrameworkArtifactId,
        List<RoutinePainSignal> recalibratedPainSignals,
        List<MechanismOpportunitySignal> recalibratedMechanismSignals,
        List<HypothesisRoutineFit> hypothesisComparison,
        List<OccupationFeedbackHistoryEntry> occupationHistory,
        double recalibratedRoutineConfidence,
        double recalibratedFrameworkConfidence,
        Map<String, Double> scoreReweighting,
        List<String> evidenceRefs,
        Instant generatedAt) {
}
