package com.marketinghub.imagegenerator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ImageGeneratorServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Garante que o extrator reconhece a imagem retornada pela ferramenta image_generation. */
    @Test
    void extractsImageGenerationResult() throws Exception {
        ImageGeneratorService service = new ImageGeneratorService(null, null, null, objectMapper, "gpt-5.6");
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
        ImageGeneratorService service = new ImageGeneratorService(null, null, null, objectMapper, "gpt-5.6");
        String payload = """
                {"id": "resp_1", "output": [{"type": "message", "content": []}]}
                """;

        assertThatThrownBy(() -> service.extractImageBase64(objectMapper.readTree(payload)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("imagem base64");
    }
}
