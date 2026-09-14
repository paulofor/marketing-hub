package com.marketinghub.worker.frameworkimage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o contrato físico de geração de imagens do framework. */
class FrameworkImageOpenAiBatchClientTest {

    /** Promove jobs antigos ao Sunburst em qualidade alta sem parâmetro incompatível. */
    @Test
    void buildsCanonicalPayloadForLegacyJob() {
        FrameworkImageOpenAiBatchClient client = new FrameworkImageOpenAiBatchClient(
                WebClient.builder(),
                "test-key",
                "http://127.0.0.1:18080",
                "gpt-image-2",
                1024 * 1024,
                Duration.ofMillis(10),
                Duration.ofSeconds(1));
        FrameworkImageJobDto job = new FrameworkImageJobDto(
                UUID.randomUUID(),
                10L,
                "hero",
                "PENDING",
                "WAITING_AI_WORKER",
                null,
                "gpt-image-1.5",
                "imagem comercial",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now());

        Map<String, Object> payload = client.buildGenerationPayload(job);

        assertThat(payload)
                .containsEntry("model", "gpt-image-2.5-sunburst")
                .containsEntry("quality", "high")
                .containsEntry("prompt", "imagem comercial")
                .doesNotContainKey("response_format");
    }
}
