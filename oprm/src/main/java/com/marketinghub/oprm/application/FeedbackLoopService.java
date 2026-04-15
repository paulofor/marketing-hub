package com.marketinghub.oprm.application;

import com.marketinghub.oprm.domain.ArtifactEnvelope;
import com.marketinghub.oprm.domain.DorResultadoOfertaMecanismoProvaInputPayload;
import com.marketinghub.oprm.domain.HypothesisPerformanceSnapshot;
import com.marketinghub.oprm.domain.HypothesisRoutineFit;
import com.marketinghub.oprm.domain.MechanismOpportunitySignal;
import com.marketinghub.oprm.domain.OccupationFeedbackHistoryEntry;
import com.marketinghub.oprm.domain.OccupationFeedbackLoopPayload;
import com.marketinghub.oprm.domain.OccupationPersonaRoutineCardPayload;
import com.marketinghub.oprm.domain.RoutinePainSignal;
import com.marketinghub.oprm.domain.RoutineTaskPattern;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FeedbackLoopService {

    private static final int MAX_HISTORY_SIZE = 12;

    private final RoutineInferenceService routineInferenceService;
    private final FrameworkIntegrationService frameworkIntegrationService;

    public FeedbackLoopService(RoutineInferenceService routineInferenceService,
                               FrameworkIntegrationService frameworkIntegrationService) {
        this.routineInferenceService = routineInferenceService;
        this.frameworkIntegrationService = frameworkIntegrationService;
    }

    public ArtifactEnvelope recalibrateWithFeedback(String rawOccupationLabel,
                                                    String nicheName,
                                                    String locale,
                                                    String correlationId,
                                                    List<HypothesisPerformanceSnapshot> performanceSnapshots) {
        return recalibrateWithFeedback(rawOccupationLabel, nicheName, locale, correlationId, performanceSnapshots, List.of());
    }

    public ArtifactEnvelope recalibrateWithFeedback(String rawOccupationLabel,
                                                    String nicheName,
                                                    String locale,
                                                    String correlationId,
                                                    List<HypothesisPerformanceSnapshot> performanceSnapshots,
                                                    List<OccupationFeedbackHistoryEntry> persistedHistory) {
        ArtifactEnvelope routineEnvelope = routineInferenceService.inferRoutine(rawOccupationLabel, nicheName, locale, correlationId);
        ArtifactEnvelope frameworkEnvelope = frameworkIntegrationService.integrateRoutineSignals(rawOccupationLabel, nicheName, locale, correlationId);

        OccupationPersonaRoutineCardPayload routinePayload = (OccupationPersonaRoutineCardPayload) routineEnvelope.payload();
        DorResultadoOfertaMecanismoProvaInputPayload frameworkPayload = (DorResultadoOfertaMecanismoProvaInputPayload) frameworkEnvelope.payload();

        List<HypothesisPerformanceSnapshot> sanitizedSnapshots = performanceSnapshots == null ? List.of() : performanceSnapshots;
        List<HypothesisRoutineFit> comparison = buildRoutineComparison(sanitizedSnapshots, routinePayload, frameworkPayload);

        double averageImpact = comparison.isEmpty()
                ? 0.5
                : comparison.stream().mapToDouble(HypothesisRoutineFit::weightedImpactScore).average().orElse(0.5);

        List<RoutinePainSignal> recalibratedPainSignals = recalibratePainSignals(routinePayload.painSignals(), averageImpact);
        List<MechanismOpportunitySignal> recalibratedMechanisms = recalibrateMechanismSignals(
                frameworkPayload.mechanismOpportunitySignals(),
                averageImpact
        );

        double recalibratedRoutineConfidence = recalibrateConfidence(routineEnvelope.confidenceScore(), averageImpact);
        double recalibratedFrameworkConfidence = recalibrateConfidence(frameworkEnvelope.confidenceScore(), averageImpact);

        OccupationFeedbackHistoryEntry historyEntry = new OccupationFeedbackHistoryEntry(
                Instant.now(),
                routineEnvelope.confidenceScore(),
                recalibratedRoutineConfidence,
                frameworkEnvelope.confidenceScore(),
                recalibratedFrameworkConfidence,
                averageImpact,
                "phase-5 feedback recalibration using hypothesis performance snapshots"
        );

        List<OccupationFeedbackHistoryEntry> occupationHistory = mergeHistory(persistedHistory, historyEntry);

        Map<String, Double> scoreReweighting = Map.of(
                "average_hypothesis_impact", averageImpact,
                "previous_routine_confidence", routineEnvelope.confidenceScore(),
                "recalibrated_routine_confidence", recalibratedRoutineConfidence,
                "previous_framework_confidence", frameworkEnvelope.confidenceScore(),
                "recalibrated_framework_confidence", recalibratedFrameworkConfidence,
                "routine_confidence_delta", recalibratedRoutineConfidence - routineEnvelope.confidenceScore(),
                "framework_confidence_delta", recalibratedFrameworkConfidence - frameworkEnvelope.confidenceScore(),
                "pain_intensity_multiplier", 0.90 + (averageImpact * 0.20),
                "mechanism_fit_multiplier", 0.88 + (averageImpact * 0.24)
        );

        OccupationFeedbackLoopPayload payload = new OccupationFeedbackLoopPayload(
                routinePayload.personaLabel(),
                routinePayload.occupationName(),
                routinePayload.nicheName(),
                routineEnvelope.artifactId(),
                frameworkEnvelope.artifactId(),
                recalibratedPainSignals,
                recalibratedMechanisms,
                comparison,
                occupationHistory,
                recalibratedRoutineConfidence,
                recalibratedFrameworkConfidence,
                scoreReweighting,
                routinePayload.evidenceRefs(),
                Instant.now()
        );

        return new ArtifactEnvelope(
                "occupationFeedbackLoopSnapshot",
                "1.0",
                UUID.randomUUID().toString(),
                "oprm",
                "oprm.feedback-loop",
                Instant.now(),
                effectiveCorrelationId(correlationId),
                UUID.randomUUID().toString(),
                routinePayload.evidenceRefs(),
                List.of(
                        routineEnvelope.artifactId(),
                        frameworkEnvelope.artifactId(),
                        "occupationPersonaRoutineCard",
                        "dorResultadoOfertaMecanismoProvaInput"
                ),
                payload,
                "GENERATED",
                Math.min(0.96, (recalibratedRoutineConfidence + recalibratedFrameworkConfidence) / 2.0),
                Map.of(
                        "phase", "phase-5",
                        "history_entries", occupationHistory.size(),
                        "hypothesis_snapshots", sanitizedSnapshots.size(),
                        "comparison_entries", comparison.size()
                )
        );
    }

    private List<HypothesisRoutineFit> buildRoutineComparison(List<HypothesisPerformanceSnapshot> snapshots,
                                                              OccupationPersonaRoutineCardPayload routinePayload,
                                                              DorResultadoOfertaMecanismoProvaInputPayload frameworkPayload) {
        if (snapshots.isEmpty()) {
            return List.of();
        }

        List<String> routineSignals = new ArrayList<>();
        routineSignals.addAll(routinePayload.topTasks().stream().map(RoutineTaskPattern::taskLabel).toList());
        routineSignals.addAll(routinePayload.painSignals().stream().map(RoutinePainSignal::painLabel).toList());
        routineSignals.addAll(frameworkPayload.mechanismOpportunitySignals().stream()
                .map(MechanismOpportunitySignal::mechanismLabel)
                .toList());

        return snapshots.stream()
                .map(snapshot -> {
                    List<String> linkedSignals = routineSignals.stream()
                            .filter(signal -> hasSemanticOverlap(snapshot.hypothesisLabel(), signal))
                            .distinct()
                            .limit(4)
                            .toList();

                    double fitScore = linkedSignals.isEmpty() ? 0.45 : Math.min(0.95, 0.50 + linkedSignals.size() * 0.10);
                    double performanceScore = calculatePerformanceScore(snapshot);
                    double weightedImpact = Math.min(0.98, (fitScore * 0.42) + (performanceScore * 0.58));

                    return new HypothesisRoutineFit(
                            snapshot.hypothesisId(),
                            snapshot.hypothesisLabel(),
                            performanceScore,
                            fitScore,
                            weightedImpact,
                            linkedSignals,
                            buildComparisonSummary(snapshot, linkedSignals, weightedImpact)
                    );
                })
                .sorted(Comparator.comparing(HypothesisRoutineFit::weightedImpactScore).reversed())
                .toList();
    }

    private String buildComparisonSummary(HypothesisPerformanceSnapshot snapshot,
                                          List<String> linkedSignals,
                                          double weightedImpact) {
        String alignment = linkedSignals.isEmpty()
                ? "baixa aderência aos sinais de rotina"
                : "aderência parcial com sinais operacionais inferidos";
        return snapshot.hypothesisLabel() + " apresenta " + alignment + " (impacto ponderado="
                + String.format(Locale.US, "%.2f", weightedImpact) + ")";
    }

    private List<RoutinePainSignal> recalibratePainSignals(List<RoutinePainSignal> painSignals, double averageImpact) {
        double intensityMultiplier = 0.90 + (averageImpact * 0.20);
        double recurrenceMultiplier = 0.92 + (averageImpact * 0.16);

        return painSignals.stream()
                .map(pain -> new RoutinePainSignal(
                        pain.painLabel(),
                        pain.painSummary(),
                        pain.painType(),
                        capScore(pain.painIntensityScore() * intensityMultiplier),
                        capScore(pain.painRecurrenceScore() * recurrenceMultiplier),
                        pain.workaroundSummary(),
                        pain.evidenceRefs()
                ))
                .toList();
    }

    private List<MechanismOpportunitySignal> recalibrateMechanismSignals(List<MechanismOpportunitySignal> mechanisms,
                                                                          double averageImpact) {
        double fitMultiplier = 0.88 + (averageImpact * 0.24);
        double effortMultiplier = Math.max(0.65, 1.05 - (averageImpact * 0.15));

        return mechanisms.stream()
                .map(mechanism -> new MechanismOpportunitySignal(
                        mechanism.mechanismLabel(),
                        mechanism.mechanismSummary(),
                        mechanism.linkedTaskRefs(),
                        mechanism.linkedPainRefs(),
                        capScore(mechanism.commercialFitScore() * fitMultiplier),
                        capScore(mechanism.implementationEffortScore() * effortMultiplier),
                        mechanism.evidenceRefs()
                ))
                .toList();
    }

    private double calculatePerformanceScore(HypothesisPerformanceSnapshot snapshot) {
        double ctrScore = capScore(snapshot.ctr() / 0.06);
        double conversionScore = capScore(snapshot.conversionRate() / 0.12);
        double cpaScore = 1.0 - capScore(snapshot.cpa() / 140.0);
        return capScore((ctrScore * 0.30) + (conversionScore * 0.45) + (cpaScore * 0.15) + (snapshot.confidenceScore() * 0.10));
    }

    private List<OccupationFeedbackHistoryEntry> mergeHistory(List<OccupationFeedbackHistoryEntry> persistedHistory,
                                                              OccupationFeedbackHistoryEntry latestEntry) {
        List<OccupationFeedbackHistoryEntry> merged = new ArrayList<>(persistedHistory == null ? List.of() : persistedHistory);
        merged.add(latestEntry);
        if (merged.size() <= MAX_HISTORY_SIZE) {
            return List.copyOf(merged);
        }
        return List.copyOf(merged.subList(merged.size() - MAX_HISTORY_SIZE, merged.size()));
    }

    private double recalibrateConfidence(double previousConfidence, double averageImpact) {
        double delta = (averageImpact - 0.50) * 0.22;
        return capScore(previousConfidence + delta);
    }

    private String effectiveCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private boolean hasSemanticOverlap(String hypothesisLabel, String routineSignal) {
        String normalizedHypothesis = hypothesisLabel == null ? "" : hypothesisLabel.toLowerCase(Locale.ROOT);
        String normalizedSignal = routineSignal == null ? "" : routineSignal.toLowerCase(Locale.ROOT);

        if (normalizedHypothesis.isBlank() || normalizedSignal.isBlank()) {
            return false;
        }

        List<String> hypothesisTokens = List.of(normalizedHypothesis.split("\\s+"));
        return hypothesisTokens.stream()
                .filter(token -> token.length() > 3)
                .anyMatch(normalizedSignal::contains);
    }

    private double capScore(double value) {
        return Math.max(0.0, Math.min(0.99, value));
    }
}
