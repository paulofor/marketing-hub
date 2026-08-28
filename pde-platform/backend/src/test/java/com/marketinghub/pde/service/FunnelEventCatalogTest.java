package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o catálogo único de eventos comerciais do PDE. */
class FunnelEventCatalogTest {

    /** Confirma que toda a jornada declarada publicamente também é aceita na ingestão. */
    @Test
    void supportsEveryRequiredCommercialJourneyEvent() {
        assertThat(FunnelEventCatalog.supportsRequiredCommercialJourney()).isTrue();
        assertThat(FunnelEventCatalog.REQUIRED_COMMERCIAL_JOURNEY_EVENTS)
                .allSatisfy(event -> assertThat(FunnelEventCatalog.normalize(event)).isEqualTo(event));
    }

    /** Rejeita evento desconhecido para impedir métricas silenciosamente incompatíveis. */
    @Test
    void rejectsUnknownEventType() {
        assertThatThrownBy(() -> FunnelEventCatalog.normalize("SALE_MAYBE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evento PDE não suportado");
    }
}
