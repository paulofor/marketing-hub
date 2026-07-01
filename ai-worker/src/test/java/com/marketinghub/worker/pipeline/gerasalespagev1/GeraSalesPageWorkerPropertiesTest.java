package com.marketinghub.worker.pipeline.gerasalespagev1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Valida defaults operacionais do worker GeraSalesPage v1. */
class GeraSalesPageWorkerPropertiesTest {
    /** Garante que o worker nasce com todas as etapas canônicas quando a configuração externa omite a lista. */
    @Test
    void shouldDefaultCanonicalStageCodes() {
        GeraSalesPageWorkerProperties properties =
                new GeraSalesPageWorkerProperties(true, 5, "http://backend", null, null, null, null);

        assertEquals("/api", properties.apiPrefix());
        assertEquals(Duration.ofMinutes(30), properties.timeout());
        assertEquals("default", properties.serviceTier());
        assertEquals(7, properties.stageCodes().size());
        assertTrue(properties.stageCodes().contains("sales-page-checkout-quality-review"));
    }

    /** Preserva lista explícita de etapas quando usada para rollout operacional. */
    @Test
    void shouldKeepExplicitStageCodes() {
        GeraSalesPageWorkerProperties properties = new GeraSalesPageWorkerProperties(
                true,
                1,
                "http://backend",
                "/api",
                List.of("sales-page-offer-brief"),
                Duration.ofMinutes(5),
                "standard");

        assertEquals(List.of("sales-page-offer-brief"), properties.stageCodes());
        assertEquals("default", properties.serviceTier());
    }
}
