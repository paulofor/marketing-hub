package com.marketinghub.worker.openai.core.qualityreview;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.StageExecution;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar o request multimodal da etapa quality-review. */
class QualityReviewPromptBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Deve enviar somente screenshots renderizados como imagens do request OpenAI, não URLs soltas do HTML. */
    @Test
    void buildShouldSendRenderedScreenshotsAsVisionInputs() throws Exception {
        QualityReviewPromptBuilder builder = new QualityReviewPromptBuilder(
                objectMapper,
                workerProperties(),
                input -> List.of(
                        new QualityReviewScreenshotEvidence(
                                "desktop",
                                "https://cdn.example.com/screens/job-quality-1-desktop.jpg",
                                "sha-desktop",
                                123),
                        new QualityReviewScreenshotEvidence(
                                "mobile",
                                "https://cdn.example.com/screens/job-quality-1-mobile.jpg",
                                "sha-mobile",
                                456)));
        StageExecution<QualityReviewInput> execution = new StageExecution<>(
                "job-quality-1",
                36L,
                "landing-page-quality-review",
                "INICIADO",
                Instant.parse("2026-06-03T03:30:00Z"),
                new QualityReviewInput(
                        36L,
                        "landing-page-quality-review",
                        "job-quality-1",
                        Map.of("htmlGeraLanding", "<!doctype html><html><body>Landing</body></html>"),
                        "<!doctype html><html><body>Landing</body></html>"));

        OpenAiRequest request = builder.build(execution);

        Map<String, Object> body = objectMapper.readValue(request.requestBodyJson(), new TypeReference<>() {});
        List<Map<String, Object>> input = (List<Map<String, Object>>) body.get("input");
        List<Map<String, Object>> content = (List<Map<String, Object>>) input.getFirst().get("content");
        assertThat(content)
                .filteredOn(item -> "input_image".equals(item.get("type")))
                .extracting(item -> item.get("image_url"))
                .containsExactly(
                        "https://cdn.example.com/screens/job-quality-1-desktop.jpg",
                        "https://cdn.example.com/screens/job-quality-1-mobile.jpg");
        assertThat(content)
                .filteredOn(item -> "input_image".equals(item.get("type")))
                .extracting(item -> item.get("image_url"))
                .doesNotContain("https://cdn.example.com/asset-hero.jpg");
        assertThat(content)
                .filteredOn(item -> "input_image".equals(item.get("type")))
                .extracting(item -> item.get("detail"))
                .containsOnly("original");
        assertThat(request.prompt())
                .contains("HTML final do GeraLanding")
                .contains("<!doctype html><html><body>Landing</body></html>")
                .contains("https://cdn.example.com/screens/job-quality-1-desktop.jpg")
                .doesNotContain("JSON da etapa wireframe")
                .doesNotContain("JSON da etapa preset design")
                .doesNotContain("landingPageWireframe")
                .doesNotContain("landingPageDesignPreset")
                .doesNotContain("CASE_DATA_BEGIN")
                .doesNotContain("https://cdn.example.com/asset-hero.jpg");
        assertThat(body.get("model")).isEqualTo("gpt-5.5");
        assertThat(request.model()).isEqualTo("gpt-5.5");
        assertThat(request.serviceTier()).isEqualTo("default");
        assertThat(request.metadata()).containsKeys("qualityReviewAudit", "idJob", "experimentId");
        Map<String, Object> audit = (Map<String, Object>) request.metadata().get("qualityReviewAudit");
        assertThat(audit)
                .containsEntry("landingHtmlLength", 48)
                .containsEntry("imageDetail", "original")
                .containsEntry("serviceTier", "default")
                .containsEntry("visionModel", "gpt-5.5");
        assertThat((List<Map<String, Object>>) audit.get("screenshots"))
                .extracting(item -> item.get("sha256"))
                .containsExactly("sha-desktop", "sha-mobile");
    }

    /** Cria propriedades operacionais mínimas para o builder quality-review. */
    private QualityReviewWorkerProperties workerProperties() {
        return new QualityReviewWorkerProperties(
                true,
                5,
                "http://backend.test",
                "/api",
                "prompts/geralanding/landing-page-quality-review.md",
                "prompts/geralanding/landing-page-quality-review-schema.json",
                "experiment_pipeline_landing_page_quality_review",
                "gpt-5.5",
                "original",
                "default",
                Duration.ofSeconds(5),
                "/usr/bin/chromium",
                Duration.ofSeconds(5),
                52_428_800);
    }
}
