package com.marketinghub.worker.openai.core.qualityreview;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar os defaults operacionais da etapa Quality Review. */
class QualityReviewWorkerPropertiesTest {

    /** Deve usar timeout maior para permitir screenshots full-page sem limitar o tamanho da imagem. */
    @Test
    void shouldDefaultScreenshotTimeoutToTwoMinutes() {
        QualityReviewWorkerProperties properties = new QualityReviewWorkerProperties(
                true,
                5,
                "http://backend.test",
                "/api",
                "prompts/geralanding/landing-page-quality-review.md",
                "prompts/geralanding/landing-page-quality-review-schema.json",
                "experiment_pipeline_landing_page_quality_review",
                "gpt-5.5",
                "original",
                null,
                Duration.ofMinutes(30),
                "/usr/bin/chromium",
                null);

        assertThat(properties.screenshotTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.serviceTier()).isEqualTo("default");
    }
}
