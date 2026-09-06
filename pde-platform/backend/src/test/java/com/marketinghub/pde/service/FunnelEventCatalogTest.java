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

    /** Mantém os sinais da homologação multiagente explícitos sem convertê-los em eventos comerciais. */
    @Test
    void supportsAgentValidationAuditEvents() {
        assertThat(FunnelEventCatalog.normalize("RECOVERY_COMPLETED")).isEqualTo("RECOVERY_COMPLETED");
        assertThat(FunnelEventCatalog.normalize("SAFETY_LIMIT_BLOCKED")).isEqualTo("SAFETY_LIMIT_BLOCKED");
        assertThat(FunnelEventCatalog.normalize("AGENT_SCENARIO_COMPLETED")).isEqualTo("AGENT_SCENARIO_COMPLETED");
        assertThat(FunnelEventCatalog.REQUIRED_COMMERCIAL_JOURNEY_EVENTS)
                .doesNotContain("RECOVERY_COMPLETED", "SAFETY_LIMIT_BLOCKED", "AGENT_SCENARIO_COMPLETED");
    }

    /** Rejeita evento desconhecido para impedir métricas silenciosamente incompatíveis. */
    @Test
    void rejectsUnknownEventType() {
        assertThatThrownBy(() -> FunnelEventCatalog.normalize("SALE_MAYBE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evento PDE não suportado");
    }
}
