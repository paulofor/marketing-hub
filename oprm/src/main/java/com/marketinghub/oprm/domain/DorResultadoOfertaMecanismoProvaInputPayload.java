package com.marketinghub.oprm.domain;

import java.time.Instant;
import java.util.List;

public record DorResultadoOfertaMecanismoProvaInputPayload(
        String personaLabel,
        String occupationName,
        String nicheName,
        List<RoutinePainSignal> painSignals,
        List<DesiredOutcomeSignal> desiredOutcomeSignals,
        List<MechanismOpportunitySignal> mechanismOpportunitySignals,
        List<String> evidenceRefs,
        List<String> originArtifactRefs,
        String integrationNotes,
        Instant generatedAt) {
}
