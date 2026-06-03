package com.marketinghub.worker.openai.core.qualityreview;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a prioridade operacional dos screenshots visuais do Quality Review. */
class PlaywrightQualityReviewScreenshotServiceTest {

    /** Deve capturar mobile antes de desktop para preservar a evidência visual prioritária. */
    @Test
    void capturePriorityViewportNamesShouldStartWithMobile() {
        assertThat(PlaywrightQualityReviewScreenshotService.capturePriorityViewportNames())
                .containsExactly("mobile", "desktop");
    }
}
