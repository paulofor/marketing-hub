package com.marketinghub.oprm.application;

import com.marketinghub.oprm.domain.ArtifactEnvelope;
import com.marketinghub.oprm.domain.DorResultadoOfertaMecanismoProvaInputPayload;
import com.marketinghub.oprm.infra.StructuredOccupationCatalog;
import com.marketinghub.oprm.infra.enrichment.FetchedWebPage;
import com.marketinghub.oprm.infra.enrichment.OccupationSourcePolicyRegistry;
import com.marketinghub.oprm.infra.enrichment.OccupationWebSeedRegistry;
import com.marketinghub.oprm.infra.enrichment.WebPageFetcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameworkIntegrationServiceTest {

    private final WebPageFetcher stubFetcher = url -> new FetchedWebPage(
            url,
            200,
            "Personal Trainer Daily Routine",
            "Montar fichas de treino, acompanhar alunos no WhatsApp e ajustar agenda.",
            "pt-BR",
            "captured-for-phase4-test",
            true
    );

    private final FrameworkIntegrationService service = new FrameworkIntegrationService(
            new RoutineInferenceService(
                    new OccupationResolverService(new StructuredOccupationCatalog()),
                    new WebEnrichmentService(
                            new StructuredOccupationCatalog(),
                            new OccupationSourcePolicyRegistry(),
                            new OccupationWebSeedRegistry(),
                            stubFetcher
                    )
            )
    );

    @Test
    void shouldGenerateFrameworkIntegrationArtifactForPhase4() {
        ArtifactEnvelope result = service.integrateRoutineSignals("treinador pessoal", "fitness", "pt-BR", "corr-phase4-1");

        assertEquals("dorResultadoOfertaMecanismoProvaInput", result.artifactType());
        assertEquals("GENERATED", result.status());

        DorResultadoOfertaMecanismoProvaInputPayload payload = (DorResultadoOfertaMecanismoProvaInputPayload) result.payload();
        assertEquals("personal trainer", payload.occupationName());
        assertTrue(payload.painSignals().size() > 0);
        assertTrue(payload.desiredOutcomeSignals().size() > 0);
        assertTrue(payload.mechanismOpportunitySignals().size() > 0);
    }
}
