package com.marketinghub.worker.openai.core.imagegeneration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.InvalidModelResponseException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar o parser da resposta da OpenAI Images API na etapa imagegeneration. */
class ImageGenerationResponseValidatorTest {

    private final ImageGenerationResponseValidator validator = new ImageGenerationResponseValidator(new ObjectMapper());

    /** Deve converter b64_json da OpenAI em bytes de imagem para publicação posterior. */
    @Test
    void validateAndParseShouldDecodeBase64Image() {
        String payload = Base64.getEncoder().encodeToString("fake-image".getBytes(StandardCharsets.UTF_8));
        String response = "{\"model\":\"gpt-image-1.5\",\"prompt\":\"hero\",\"data\":[{\"b64_json\":\"" + payload + "\"}]}";

        ImageGenerationOutput output = validator.validateAndParse(response);

        assertThat(output.model()).isEqualTo("gpt-image-1.5");
        assertThat(output.prompt()).isEqualTo("hero");
        assertThat(output.imageContent()).isEqualTo("fake-image".getBytes(StandardCharsets.UTF_8));
        assertThat(output.imageUrl()).isNull();
    }

    /** Deve rejeitar resposta sem bytes ou URL para bloquear publicação sem imagem final. */
    @Test
    void validateAndParseShouldRejectResponseWithoutImage() {
        assertThatThrownBy(() -> validator.validateAndParse("{\"data\":[{}]}"))
                .isInstanceOf(InvalidModelResponseException.class)
                .hasMessageContaining("b64_json or url");
    }
}
