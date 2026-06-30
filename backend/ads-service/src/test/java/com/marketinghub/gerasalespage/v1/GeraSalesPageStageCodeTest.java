package com.marketinghub.gerasalespage.v1;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Valida a ordem canônica de encadeamento do GeraSalesPage v1. */
class GeraSalesPageStageCodeTest {
    /** Garante que a primeira etapa avança para wireframe sem depender do GeraLanding. */
    @Test
    void shouldResolveNextStage() {
        assertThat(GeraSalesPageStageCode.nextAfter("sales-page-offer-brief"))
                .contains("sales-page-wireframe");
    }

    /** Garante que a etapa final não tenta enfileirar avanço inexistente. */
    @Test
    void shouldStopAfterPublicationPackage() {
        assertThat(GeraSalesPageStageCode.nextAfter("sales-page-publication-package")).isEmpty();
    }
}
