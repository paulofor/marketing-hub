package com.marketinghub.oprm.application;

import com.marketinghub.oprm.domain.ArtifactEnvelope;
import com.marketinghub.oprm.domain.OccupationWebSourceSnapshotPayload;
import com.marketinghub.oprm.infra.StructuredOccupationCatalog;
import com.marketinghub.oprm.infra.enrichment.FetchedWebPage;
import com.marketinghub.oprm.infra.enrichment.OccupationSourcePolicyRegistry;
import com.marketinghub.oprm.infra.enrichment.OccupationWebSeedRegistry;
import com.marketinghub.oprm.infra.enrichment.WebPageFetcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebEnrichmentServiceTest {

    private final WebPageFetcher stubFetcher = url -> new FetchedWebPage(
            url,
            200,
            "Personal Trainer Daily Routine",
            "Montar fichas de treino e acompanhar alunos usando WhatsApp e apps de treino.",
            "pt-BR",
            "captured-for-test",
            true
    );

    private final WebEnrichmentService service = new WebEnrichmentService(
            new StructuredOccupationCatalog(),
            new OccupationSourcePolicyRegistry(),
            new OccupationWebSeedRegistry(),
            stubFetcher
    );

    @Test
    void shouldGeneratePhase2WebSourceSnapshot() {
        ArtifactEnvelope result = service.enrichOccupation("treinador pessoal", "fitness", "pt-BR", "corr-phase2-1");

        assertEquals("occupationWebSourceSnapshot", result.artifactType());
        assertEquals("GENERATED", result.status());

        OccupationWebSourceSnapshotPayload payload = (OccupationWebSourceSnapshotPayload) result.payload();
        assertEquals("personal trainer", payload.occupationName());
        assertEquals(2, payload.capturedSources().size());
        assertTrue(payload.semanticRoutineSignals().stream().anyMatch(signal -> signal.startsWith("task:")));
    }

    @Test
    void shouldRejectUnsupportedOccupationInPhase2() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enrichOccupation("astronauta", "espacial", "pt-BR", null));
    }
}
