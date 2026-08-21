package com.marketinghub.leadportal.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato de privacidade do coletor Clarity público. */
class ClarityTrackingScriptFactoryTest {

    /**
     * Injeta antes do fechamento do head com armazenamento negado e formulário mascarado.
     */
    @Test
    void injectsAggregateNoConsentCollectorInHead() {
        ClarityTrackingScriptFactory factory = new ClarityTrackingScriptFactory("project-123");

        String result = factory.inject("<!doctype html><html><head></head><body><form></form></body></html>");

        assertThat(result)
                .contains("data-mh-clarity-analytics=\"aggregate-v1\"")
                .contains("analytics_Storage: 'denied'")
                .contains("data-clarity-mask")
                .contains("DOMContentLoaded", "startMaskedCollection")
                .contains("mh_test", "mh_audit", "mh_internal_test")
                .doesNotContain("identify", "sessionId", "visitorId");
        assertThat(result.indexOf("data-mh-clarity-analytics")).isLessThan(result.indexOf("</head>"));
    }

    /**
     * Mantém o HTML inalterado quando o projeto não está configurado ou já foi injetado.
     */
    @Test
    void disablesCollectorWithoutValidProjectAndAvoidsDuplication() {
        String html = "<html><head></head><body></body></html>";
        assertThat(new ClarityTrackingScriptFactory("").inject(html)).isEqualTo(html);
        assertThat(new ClarityTrackingScriptFactory("valor inválido").inject(html)).isEqualTo(html);

        ClarityTrackingScriptFactory enabled = new ClarityTrackingScriptFactory("project-123");
        String once = enabled.inject(html);
        assertThat(enabled.inject(once).split("data-mh-clarity-analytics", -1)).hasSize(2);
    }
}
