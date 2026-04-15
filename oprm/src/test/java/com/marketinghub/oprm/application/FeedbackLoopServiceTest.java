package com.marketinghub.oprm.application;

import com.marketinghub.oprm.domain.ArtifactEnvelope;
import com.marketinghub.oprm.domain.HypothesisPerformanceSnapshot;
import com.marketinghub.oprm.domain.OccupationFeedbackLoopPayload;
import com.marketinghub.oprm.infra.StructuredOccupationCatalog;
import com.marketinghub.oprm.infra.enrichment.FetchedWebPage;
import com.marketinghub.oprm.infra.enrichment.OccupationSourcePolicyRegistry;
import com.marketinghub.oprm.infra.enrichment.OccupationWebSeedRegistry;
import com.marketinghub.oprm.infra.enrichment.WebPageFetcher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackLoopServiceTest {

    private final WebPageFetcher stubFetcher = url -> new FetchedWebPage(
            url,
            200,
            "Routine Signals for Personal Trainer",
            "Organizar agenda, mandar lembrete de treino e responder clientes no WhatsApp.",
            "pt-BR",
            "captured-for-phase5-test",
            true
    );

    private final FeedbackLoopService service = new FeedbackLoopService(
            new RoutineInferenceService(
                    new OccupationResolverService(new StructuredOccupationCatalog()),
                    new WebEnrichmentService(
                            new StructuredOccupationCatalog(),
                            new OccupationSourcePolicyRegistry(),
                            new OccupationWebSeedRegistry(),
                            stubFetcher
                    )
            ),
            new FrameworkIntegrationService(
                    new RoutineInferenceService(
                            new OccupationResolverService(new StructuredOccupationCatalog()),
                            new WebEnrichmentService(
                                    new StructuredOccupationCatalog(),
                                    new OccupationSourcePolicyRegistry(),
                                    new OccupationWebSeedRegistry(),
                                    stubFetcher
                            )
                    )
            )
    );

    @Test
    void shouldGenerateFeedbackLoopArtifactWithHistoryAndComparison() {
        List<HypothesisPerformanceSnapshot> snapshots = List.of(
                new HypothesisPerformanceSnapshot("hyp-1", "Checklist para rotina de personal trainer", 0.041, 0.087, 79.0, 0.81),
                new HypothesisPerformanceSnapshot("hyp-2", "Automação de lembretes de agenda", 0.037, 0.072, 88.0, 0.76)
        );

        ArtifactEnvelope result = service.recalibrateWithFeedback(
                "treinador pessoal",
                "fitness",
                "pt-BR",
                "corr-phase5-1",
                snapshots
        );

        assertEquals("occupationFeedbackLoopSnapshot", result.artifactType());
        assertEquals("GENERATED", result.status());

        OccupationFeedbackLoopPayload payload = (OccupationFeedbackLoopPayload) result.payload();
        assertEquals("personal trainer", payload.occupationName());
        assertEquals(2, payload.hypothesisComparison().size());
        assertTrue(payload.occupationHistory().size() >= 1);
        assertTrue(payload.recalibratedFrameworkConfidence() > 0.0);
    }

    @Test
    void shouldAccumulateHistoryByOccupationAcrossExecutions() {
        ArtifactEnvelope firstRun = service.recalibrateWithFeedback(
                "treinador pessoal",
                "fitness",
                "pt-BR",
                "corr-phase5-2",
                List.of()
        );

        OccupationFeedbackLoopPayload firstPayload = (OccupationFeedbackLoopPayload) firstRun.payload();

        ArtifactEnvelope secondRun = service.recalibrateWithFeedback(
                "personal trainer",
                "fitness",
                "pt-BR",
                "corr-phase5-3",
                List.of(),
                firstPayload.occupationHistory()
        );

        OccupationFeedbackLoopPayload secondPayload = (OccupationFeedbackLoopPayload) secondRun.payload();

        assertEquals(1, firstPayload.occupationHistory().size());
        assertEquals(2, secondPayload.occupationHistory().size());
    }
}
