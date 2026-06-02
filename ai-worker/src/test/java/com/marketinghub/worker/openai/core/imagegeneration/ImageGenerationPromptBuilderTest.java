package com.marketinghub.worker.openai.core.imagegeneration;

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

/** Responsabilidade: validar a montagem do request de geração de imagens no padrão core OpenAI. */
class ImageGenerationPromptBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Deve montar payload auditável usando os prompts planejados recebidos do endpoint GeraLanding. */
    @Test
    void buildShouldUseGeraLandingPlannedImagePrompts() throws Exception {
        ImageGenerationPromptBuilder builder = new ImageGenerationPromptBuilder(objectMapper, properties());

        OpenAiRequest request = builder.build(new StageExecution<>(
                "job-123",
                33L,
                "landing-page-image-generation",
                "INICIADO",
                Instant.parse("2026-05-31T10:00:00Z"),
                new ImageGenerationInput(
                        33L,
                        "landing-page-image-generation",
                        "job-123",
                        List.of(new ImageGenerationInput.ImageGenerationPromptItem(
                                "hero-img",
                                "hero",
                                "hero-img",
                                "Mostrar produto digital",
                                "Crie imagem hero de transformação real")))));

        Map<String, Object> body = objectMapper.readValue(request.requestBodyJson(), new TypeReference<>() {});
        assertThat(body)
                .containsEntry("model", "gpt-image-1.5")
                .containsEntry("responseFormat", "default");
        assertThat((List<Map<String, Object>>) body.get("images"))
                .singleElement()
                .satisfies(image -> assertThat(image)
                        .containsEntry("planningItemKey", "hero-img")
                        .containsEntry("sectionId", "hero")
                        .containsEntry("elementId", "hero-img")
                        .containsEntry("prompt", "Crie imagem hero de transformação real"));
        assertThat(request.metadata())
                .containsEntry("stageCode", "landing-page-image-generation")
                .containsEntry("experimentId", 33L)
                .containsEntry("imageCount", 1);
    }

    /** Cria propriedades mínimas para o builder de imagegeneration. */
    private ImageGenerationWorkerProperties properties() {
        return new ImageGenerationWorkerProperties(
                true,
                20,
                "http://backend",
                "/api",
                "gpt-image-1.5",
                Duration.ofSeconds(5),
                3,
                Duration.ofMillis(300),
                "worker-test",
                100,
                0.01d);
    }
}
