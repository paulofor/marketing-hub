package com.marketinghub.oprm.application;

import com.marketinghub.oprm.domain.ArtifactEnvelope;
import com.marketinghub.oprm.domain.DesiredOutcomeSignal;
import com.marketinghub.oprm.domain.DorResultadoOfertaMecanismoProvaInputPayload;
import com.marketinghub.oprm.domain.MechanismOpportunitySignal;
import com.marketinghub.oprm.domain.OccupationPersonaRoutineCardPayload;
import com.marketinghub.oprm.domain.RoutinePainSignal;
import com.marketinghub.oprm.domain.RoutineTaskPattern;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FrameworkIntegrationService {

    private final RoutineInferenceService routineInferenceService;

    public FrameworkIntegrationService(RoutineInferenceService routineInferenceService) {
        this.routineInferenceService = routineInferenceService;
    }

    public ArtifactEnvelope integrateRoutineSignals(String rawOccupationLabel,
                                                    String nicheName,
                                                    String locale,
                                                    String correlationId) {
        ArtifactEnvelope routineCard = routineInferenceService.inferRoutine(rawOccupationLabel, nicheName, locale, correlationId);
        OccupationPersonaRoutineCardPayload routinePayload = (OccupationPersonaRoutineCardPayload) routineCard.payload();

        List<DesiredOutcomeSignal> desiredOutcomeSignals = buildDesiredOutcomeSignals(routinePayload.painSignals());
        List<MechanismOpportunitySignal> mechanismSignals = buildMechanismSignals(
                routinePayload.topTasks(),
                routinePayload.painSignals(),
                desiredOutcomeSignals,
                routinePayload.evidenceRefs()
        );

        DorResultadoOfertaMecanismoProvaInputPayload integrationPayload = new DorResultadoOfertaMecanismoProvaInputPayload(
                routinePayload.personaLabel(),
                routinePayload.occupationName(),
                routinePayload.nicheName(),
                routinePayload.painSignals(),
                desiredOutcomeSignals,
                mechanismSignals,
                routinePayload.evidenceRefs(),
                List.of(
                        routineCard.artifactId(),
                        "desiredOutcomeSignal",
                        "mechanismOpportunitySignal",
                        "occupationPersonaRoutineCard"
                ),
                "OPRM phase 4 integration package for dor→resultado→oferta→mecanismo→prova framework",
                Instant.now()
        );

        String effectiveCorrelationId = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;

        return new ArtifactEnvelope(
                "dorResultadoOfertaMecanismoProvaInput",
                "1.0",
                UUID.randomUUID().toString(),
                "oprm",
                "oprm.framework-integration",
                Instant.now(),
                effectiveCorrelationId,
                UUID.randomUUID().toString(),
                routinePayload.evidenceRefs(),
                List.of(
                        routineCard.artifactId(),
                        "desiredOutcomeSignal",
                        "mechanismOpportunitySignal"
                ),
                integrationPayload,
                "GENERATED",
                calculateIntegrationConfidence(routinePayload.painSignals(), desiredOutcomeSignals, mechanismSignals),
                java.util.Map.of(
                        "phase", "phase-4",
                        "pain_signals", routinePayload.painSignals().size(),
                        "desired_outcome_signals", desiredOutcomeSignals.size(),
                        "mechanism_opportunity_signals", mechanismSignals.size()
                )
        );
    }

    private List<DesiredOutcomeSignal> buildDesiredOutcomeSignals(List<RoutinePainSignal> painSignals) {
        if (painSignals.isEmpty()) {
            return List.of();
        }

        return painSignals.stream()
                .limit(3)
                .map(pain -> new DesiredOutcomeSignal(
                        "outcome for " + pain.painType().toLowerCase(Locale.ROOT),
                        "reduce recurring friction caused by " + pain.painLabel().toLowerCase(Locale.ROOT),
                        List.of(pain.painLabel()),
                        Math.min(0.95, (pain.painIntensityScore() + pain.painRecurrenceScore()) / 2.0 + 0.08),
                        pain.evidenceRefs()
                ))
                .toList();
    }

    private List<MechanismOpportunitySignal> buildMechanismSignals(List<RoutineTaskPattern> taskPatterns,
                                                                    List<RoutinePainSignal> painSignals,
                                                                    List<DesiredOutcomeSignal> desiredOutcomeSignals,
                                                                    List<String> evidenceRefs) {
        if (painSignals.isEmpty() || taskPatterns.isEmpty()) {
            return List.of();
        }

        List<String> linkedTasks = taskPatterns.stream().limit(3).map(RoutineTaskPattern::taskLabel).toList();
        List<String> linkedPains = painSignals.stream().limit(3).map(RoutinePainSignal::painLabel).toList();

        return desiredOutcomeSignals.stream()
                .limit(3)
                .map(outcome -> new MechanismOpportunitySignal(
                        "mechanism for " + outcome.outcomeLabel().toLowerCase(Locale.ROOT),
                        "introduce workflow guardrails and automation to sustain "
                                + outcome.outcomeLabel().toLowerCase(Locale.ROOT),
                        linkedTasks,
                        linkedPains,
                        0.84,
                        0.46,
                        evidenceRefs
                ))
                .toList();
    }

    private double calculateIntegrationConfidence(List<RoutinePainSignal> painSignals,
                                                  List<DesiredOutcomeSignal> desiredSignals,
                                                  List<MechanismOpportunitySignal> mechanismSignals) {
        double base = 0.66;
        double painContribution = Math.min(0.10, painSignals.size() * 0.03);
        double desiredContribution = Math.min(0.10, desiredSignals.size() * 0.03);
        double mechanismContribution = Math.min(0.10, mechanismSignals.size() * 0.03);
        return Math.min(0.95, base + painContribution + desiredContribution + mechanismContribution);
    }
}
