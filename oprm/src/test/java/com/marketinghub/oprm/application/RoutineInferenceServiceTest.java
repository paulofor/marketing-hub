package com.marketinghub.oprm.application;

import com.marketinghub.oprm.domain.ArtifactEnvelope;
import com.marketinghub.oprm.domain.OccupationPersonaRoutineCardPayload;
import com.marketinghub.oprm.infra.StructuredOccupationCatalog;
import com.marketinghub.oprm.infra.enrichment.FetchedWebPage;
import com.marketinghub.oprm.infra.enrichment.OccupationSourcePolicyRegistry;
import com.marketinghub.oprm.infra.enrichment.OccupationWebSeedRegistry;
import com.marketinghub.oprm.infra.enrichment.WebPageFetcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutineInferenceServiceTest {

    private final WebPageFetcher stubFetcher = url -> new FetchedWebPage(
            url,
            200,
            "Personal Trainer Daily Routine",
            "Montar fichas de treino, acompanhar alunos no WhatsApp e ajustar agenda.",
            "pt-BR",
            "captured-for-phase3-test",
            true
    );

    private final RoutineInferenceService service = new RoutineInferenceService(
            new OccupationResolverService(new StructuredOccupationCatalog()),
            new WebEnrichmentService(
                    new StructuredOccupationCatalog(),
                    new OccupationSourcePolicyRegistry(),
                    new OccupationWebSeedRegistry(),
                    stubFetcher
            )
    );

    @Test
    void shouldGenerateOccupationPersonaRoutineCardForPhase3() {
        ArtifactEnvelope result = service.inferRoutine("treinador pessoal", "fitness", "pt-BR", "corr-phase3-1");

        assertEquals("occupationPersonaRoutineCard", result.artifactType());
        assertEquals("GENERATED", result.status());

        OccupationPersonaRoutineCardPayload payload = (OccupationPersonaRoutineCardPayload) result.payload();
        assertEquals("personal trainer", payload.occupationName());
        assertTrue(payload.topTasks().size() > 0);
        assertTrue(payload.painSignals().size() > 0);
        assertTrue(payload.workaroundPatterns().size() > 0);
    }
}
