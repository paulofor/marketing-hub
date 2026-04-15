package com.marketinghub.oprm.application;

import com.marketinghub.oprm.domain.ArtifactEnvelope;
import com.marketinghub.oprm.domain.OccupationProfileSnapshotPayload;
import com.marketinghub.oprm.infra.StructuredOccupationCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OccupationResolverServiceTest {

    private final OccupationResolverService service = new OccupationResolverService(new StructuredOccupationCatalog());

    @Test
    void shouldResolveAliasAndGenerateOccupationProfileSnapshot() {
        ArtifactEnvelope result = service.resolveToProfileSnapshot("treinador pessoal", "fitness", "pt-BR", "corr-1");

        assertEquals("occupationProfileSnapshot", result.artifactType());
        OccupationProfileSnapshotPayload payload = (OccupationProfileSnapshotPayload) result.payload();
        assertEquals("personal trainer", payload.occupationName());
        assertEquals("ALIAS", payload.aliasResolution().matchType());
        assertEquals("GENERATED", result.status());
    }

    @Test
    void shouldRejectUnsupportedOccupation() {
        assertThrows(IllegalArgumentException.class,
                () -> service.resolveToProfileSnapshot("astronauta", "espacial", "pt-BR", null));
    }
}
