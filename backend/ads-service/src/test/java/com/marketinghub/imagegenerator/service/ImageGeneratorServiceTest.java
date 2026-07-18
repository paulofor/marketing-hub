package com.marketinghub.imagegenerator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ImageGeneratorServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ImageDerivativeService derivativeService = new ImageDerivativeService();

    /** Garante que o extrator reconhece a imagem retornada pela ferramenta image_generation. */
    @Test
    void extractsImageGenerationResult() throws Exception {
        ImageGeneratorService service = new ImageGeneratorService(null, null, null, derivativeService, objectMapper, "gpt-5.6", "gpt-image-2");
        String payload = """
                {
                  "id": "resp_1",
                  "output": [
                    {"type": "message", "content": []},
                    {"type": "image_generation_call", "result": "abc123"}
                  ]
                }
                """;

        assertThat(service.extractImageBase64(objectMapper.readTree(payload))).isEqualTo("abc123");
    }

    /** Garante que resposta sem imagem não seja tratada como sucesso funcional. */
    @Test
    void rejectsResponseWithoutImage() throws Exception {
        ImageGeneratorService service = new ImageGeneratorService(null, null, null, derivativeService, objectMapper, "gpt-5.6", "gpt-image-2");
        String payload = """
                {"id": "resp_1", "output": [{"type": "message", "content": []}]}
                """;

        assertThatThrownBy(() -> service.extractImageBase64(objectMapper.readTree(payload)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("imagem base64");
    }

    /** Garante que a chamada da ferramenta solicite geração explícita e obrigatória de nova imagem. */
    @Test
    void buildsImageGenerationToolWithGenerateAction() {
        ImageGeneratorService service = new ImageGeneratorService(null, null, null, derivativeService, objectMapper, "gpt-5.6", "gpt-image-2");

        Map<String, Object> requestBody = service.buildRequestBody("Gerar imagem de teste");

        assertThat(requestBody)
                .containsEntry("model", "gpt-5.6")
                .containsEntry("service_tier", "flex")
                .containsEntry("tool_choice", Map.of("type", "image_generation"));
        assertThat(requestBody.get("tools")).isInstanceOf(List.class);
        Object firstTool = ((List<?>) requestBody.get("tools")).get(0);
        assertThat(firstTool)
                .isInstanceOf(Map.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.map(String.class, Object.class))
                .containsEntry("type", "image_generation")
                .containsEntry("action", "generate")
                .containsEntry("output_format", "png");
    }

    /** Garante que a geração padrão não force modelo específico na ferramenta de imagem. */
    @Test
    void doesNotInjectComparisonImageModelIntoDefaultTool() {
        ImageGeneratorService service = new ImageGeneratorService(null, null, null, derivativeService, objectMapper, "gpt-5.6", "gpt-image-2");

        Map<String, Object> requestBody = service.buildRequestBody("Gerar imagem de teste");

        Object firstTool = ((List<?>) requestBody.get("tools")).get(0);
        assertThat(firstTool)
                .isInstanceOf(Map.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.map(String.class, Object.class))
                .containsEntry("type", "image_generation")
                .containsEntry("action", "generate")
                .containsEntry("output_format", "png")
                .doesNotContainKey("model");
    }

    /** Garante que a variação comparativa use o modelo image2 na ferramenta de imagem. */
    @Test
    void injectsComparisonImageModelIntoTool() {
        ImageGeneratorService service = new ImageGeneratorService(null, null, null, derivativeService, objectMapper, "gpt-5.6", "gpt-image-2");

        Map<String, Object> requestBody = service.buildRequestBody("Gerar imagem de teste", "gpt-image-2");

        Object firstTool = ((List<?>) requestBody.get("tools")).get(0);
        assertThat(firstTool)
                .isInstanceOf(Map.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.map(String.class, Object.class))
                .containsEntry("type", "image_generation")
                .containsEntry("action", "generate")
                .containsEntry("model", "gpt-image-2")
                .containsEntry("output_format", "png");
    }
}
