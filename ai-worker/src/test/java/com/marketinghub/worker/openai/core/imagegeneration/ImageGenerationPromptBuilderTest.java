package com.marketinghub.worker.openai.core.imagegeneration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.frameworkimage.FrameworkImageJobDto;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.StageExecution;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a montagem do request de geração de imagens no padrão core OpenAI. */
class ImageGenerationPromptBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Deve montar payload da Images API usando o modelo padrão quando o job vem com alias legado. */
    @Test
    void buildShouldUseConfiguredImageModelForLegacyAlias() throws Exception {
        UUID jobId = UUID.randomUUID();
        FrameworkImageJobDto job = new FrameworkImageJobDto(
                jobId,
                33L,
                "hero",
                "INICIADO",
                "WAITING_AI_WORKER",
                null,
                "gpt-image-1",
                "Crie imagem hero de transformação real",
                null,
                77L,
                null,
                null,
                null,
                null,
                null,
                Instant.parse("2026-05-31T10:00:00Z"),
                null);
        ImageGenerationPromptBuilder builder = new ImageGenerationPromptBuilder(objectMapper, properties());

        OpenAiRequest request = builder.build(new StageExecution<>(
                jobId.toString(),
                33L,
                "framework-image-generation",
                "INICIADO",
                Instant.parse("2026-05-31T10:00:00Z"),
                new ImageGenerationInput(job)));

        Map<String, Object> body = objectMapper.readValue(request.requestBodyJson(), new TypeReference<>() {});
        assertThat(body)
                .containsEntry("model", "gpt-image-1.5")
                .containsEntry("prompt", "Crie imagem hero de transformação real");
        assertThat(body).doesNotContainKey("response_format");
        assertThat(request.metadata())
                .containsEntry("stageCode", "framework-image-generation")
                .containsEntry("experimentId", 33L)
                .containsEntry("assetId", 77L);
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
