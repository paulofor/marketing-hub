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
                        "https://cdn.example.com/screens/job-quality-1-desktop.jpg",
                        "https://cdn.example.com/screens/job-quality-1-mobile.jpg"));
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
                        Map.of(
                                "CASE_DATA_BLOCK", "[CASE_DATA_BEGIN]\nlandingPageImageAssets: https://cdn.example.com/asset-hero.jpg\n[CASE_DATA_END]",
                                "landingPageImageAssets", Map.of("sourceUrl", "https://cdn.example.com/asset-hero.jpg")),
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
        assertThat(body.get("model")).isEqualTo("gpt-5.5");
        assertThat(request.model()).isEqualTo("gpt-5.5");
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
                Duration.ofSeconds(5),
                "/usr/bin/chromium",
                Duration.ofSeconds(5));
    }
}
