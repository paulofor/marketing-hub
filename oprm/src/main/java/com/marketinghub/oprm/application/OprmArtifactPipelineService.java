package com.marketinghub.oprm.application;

import com.marketinghub.oprm.domain.ArtifactEnvelope;
import com.marketinghub.oprm.domain.DesiredOutcomeSignal;
import com.marketinghub.oprm.domain.DorResultadoOfertaMecanismoProvaInputPayload;
import com.marketinghub.oprm.domain.MechanismOpportunitySignal;
import com.marketinghub.oprm.domain.OccupationPersonaRoutineCardPayload;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OprmArtifactPipelineService {
    private final OccupationResolverService occupationResolverService;
    private final WebEnrichmentService webEnrichmentService;
    private final RoutineInferenceService routineInferenceService;
    private final FrameworkIntegrationService frameworkIntegrationService;

    public OprmArtifactPipelineService(OccupationResolverService occupationResolverService,
                                       WebEnrichmentService webEnrichmentService,
                                       RoutineInferenceService routineInferenceService,
                                       FrameworkIntegrationService frameworkIntegrationService) {
        this.occupationResolverService = occupationResolverService;
        this.webEnrichmentService = webEnrichmentService;
        this.routineInferenceService = routineInferenceService;
        this.frameworkIntegrationService = frameworkIntegrationService;
    }

    public List<ArtifactEnvelope> runPipeline(String occupationSeedRef,
                                              String nicheName,
                                              String locale,
                                              String correlationId) {
        ArtifactEnvelope profileSnapshot = occupationResolverService.resolveToProfileSnapshot(
                occupationSeedRef,
                nicheName,
                locale,
                correlationId
        );

        ArtifactEnvelope webSnapshot = webEnrichmentService.enrichOccupation(
                occupationSeedRef,
                nicheName,
                locale,
                correlationId
        );

        ArtifactEnvelope routineCard = routineInferenceService.inferRoutine(
                occupationSeedRef,
                nicheName,
                locale,
                correlationId
        );

        ArtifactEnvelope frameworkInput = frameworkIntegrationService.integrateRoutineSignals(
                occupationSeedRef,
                nicheName,
                locale,
                correlationId
        );

        List<ArtifactEnvelope> result = new ArrayList<>();
        result.add(profileSnapshot);
        result.add(webSnapshot);
        result.add(routineCard);

        OccupationPersonaRoutineCardPayload routinePayload = (OccupationPersonaRoutineCardPayload) routineCard.payload();
        DorResultadoOfertaMecanismoProvaInputPayload frameworkPayload =
                (DorResultadoOfertaMecanismoProvaInputPayload) frameworkInput.payload();

        result.add(buildDesiredOutcomeArtifact(frameworkPayload.desiredOutcomeSignals(), routineCard, routinePayload));
        result.add(buildMechanismOpportunityArtifact(frameworkPayload.mechanismOpportunitySignals(), routineCard, routinePayload));
        result.add(frameworkInput);

        return result;
    }

    private ArtifactEnvelope buildDesiredOutcomeArtifact(List<DesiredOutcomeSignal> desiredOutcomeSignals,
                                                         ArtifactEnvelope routineCard,
                                                         OccupationPersonaRoutineCardPayload routinePayload) {
        return new ArtifactEnvelope(
                "desiredOutcomeSignal",
                "1.0",
                UUID.randomUUID().toString(),
                "oprm",
                "oprm.framework-integration",
                Instant.now(),
                routineCard.correlationId(),
                UUID.randomUUID().toString(),
                routinePayload.evidenceRefs(),
                List.of(routineCard.artifactId()),
                Map.of("signals", desiredOutcomeSignals),
                "GENERATED",
                0.84,
                Map.of("phase", "phase-4", "signal_count", desiredOutcomeSignals.size())
        );
    }

    private ArtifactEnvelope buildMechanismOpportunityArtifact(List<MechanismOpportunitySignal> mechanismSignals,
                                                               ArtifactEnvelope routineCard,
                                                               OccupationPersonaRoutineCardPayload routinePayload) {
        return new ArtifactEnvelope(
                "mechanismOpportunitySignal",
                "1.0",
                UUID.randomUUID().toString(),
                "oprm",
                "oprm.framework-integration",
                Instant.now(),
                routineCard.correlationId(),
                UUID.randomUUID().toString(),
                routinePayload.evidenceRefs(),
                List.of(routineCard.artifactId(), "desiredOutcomeSignal"),
                Map.of("signals", mechanismSignals),
                "GENERATED",
                0.82,
                Map.of("phase", "phase-4", "signal_count", mechanismSignals.size())
        );
    }
}
