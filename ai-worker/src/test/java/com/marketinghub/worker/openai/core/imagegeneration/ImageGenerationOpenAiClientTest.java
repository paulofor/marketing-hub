package com.marketinghub.worker.openai.core.imagegeneration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o payload real enviado à OpenAI pela etapa de imagens. */
class ImageGenerationOpenAiClientTest {

    /** Garante que modelo e qualidade do contrato lógico cheguem à chamada física. */
    @Test
    void buildsPhysicalRequestWithCanonicalModelAndQuality() {
        ImageGenerationOpenAiClient client = new ImageGenerationOpenAiClient(
                WebClient.builder(),
                new ObjectMapper(),
                properties(),
                "test-key",
                "http://127.0.0.1:18080",
                true,
                1024 * 1024);

        Map<String, Object> body = client.buildOpenAiImageBody(
                "gpt-image-2.5-sunburst", "high", "imagem comercial", "default");

        assertThat(body)
                .containsEntry("model", "gpt-image-2.5-sunburst")
                .containsEntry("quality", "high")
                .containsEntry("prompt", "imagem comercial")
                .doesNotContainKey("response_format");
    }

    /** Cria a configuração mínima necessária para montar o client sem executar rede. */
    private ImageGenerationWorkerProperties properties() {
        return new ImageGenerationWorkerProperties(
                true,
                1,
                "http://backend",
                "/api",
                "gpt-image-2.5-sunburst",
                Duration.ofSeconds(5),
                3,
                Duration.ofMillis(300),
                "worker-test",
                100,
                0d);
    }
}
