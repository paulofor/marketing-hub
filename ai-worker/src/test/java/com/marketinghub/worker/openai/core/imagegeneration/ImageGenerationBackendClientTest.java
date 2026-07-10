package com.marketinghub.worker.openai.core.imagegeneration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.creative.CreativeImageOptimizer;
import com.marketinghub.worker.frameworkimage.FrameworkImageStorageClient;
import com.marketinghub.worker.openai.core.model.StageExecution;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o contrato HTTP do client imagegeneration do core OpenAI com GeraLanding. */
class ImageGenerationBackendClientTest {

    private MockWebServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Inicializa o backend simulado usado para capturar os payloads enviados pelo client. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o backend simulado após cada teste do client. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve consumir o endpoint novo de pendências do GeraLanding e extrair prompts planejados de imagem. */
    @Test
    void listPendingShouldUseGeraLandingImageGenerationEndpoint() throws Exception {
        Map<String, Object> pendingExecution = Map.of(
                "experimentId", 36,
                "stageCode", "landing-page-image-generation",
                "jobid", "job-geralanding-image-1",
                "executionRequestedAt", "2026-06-02T04:38:04.746Z",
                "experiment", Map.of(
                        "landingPageImagePlanning", Map.of(
                                "landingPageImagePlanning", Map.of(
                                        "images", List.of(Map.of(
                                                "sectionId", "hero",
                                                "elementId", "hero-img",
                                                "imageGoal", "Mostrar entrega",
                                                "imagePrompt", "Mockup premium da amostra personalizada"))))));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(List.of(pendingExecution))));
        ImageGenerationBackendClient client = newClient();

        List<StageExecution<ImageGenerationInput>> result = client.listPending(5);

        assertThat(server.takeRequest().getPath())
                .isEqualTo("/api/internal/geralanding/image-generation/stage-executions/pending");
        assertThat(result).hasSize(1);
        StageExecution<ImageGenerationInput> execution = result.getFirst();
        assertThat(execution.idJob()).isEqualTo("job-geralanding-image-1");
        assertThat(execution.stageCode()).isEqualTo("landing-page-image-generation");
        assertThat(execution.aggregateId()).isEqualTo(36L);
        assertThat(execution.input().images()).singleElement().satisfies(image -> {
            assertThat(image.sectionId()).isEqualTo("hero");
            assertThat(image.elementId()).isEqualTo("hero-img");
            assertThat(image.prompt()).isEqualTo("Mockup premium da amostra personalizada");
        });
    }

    /** Deve aceitar payload de pending maior que o limite padrão do WebClient para não travar GeraLanding. */
    @Test
    void listPendingShouldAcceptLargeGeraLandingPayload() throws Exception {
        Map<String, Object> pendingExecution = Map.of(
                "experimentId", 63,
                "stageCode", "landing-page-image-generation",
                "jobid", "job-geralanding-image-large",
                "executionRequestedAt", "2026-07-10T12:00:00Z",
                "landingPageDesignPreset", "x".repeat(350_000),
                "experiment", Map.of(
                        "landingPageImagePlanning", Map.of(
                                "landingPageImagePlanning", Map.of(
                                        "images", List.of(Map.of(
                                                "sectionId", "hero",
                                                "elementId", "hero-img",
                                                "imageGoal", "Mostrar entrega",
                                                "imagePrompt", "Imagem principal da promessa"))))));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(List.of(pendingExecution))));
        ImageGenerationBackendClient client = newClient();

        List<StageExecution<ImageGenerationInput>> result = client.listPending(5);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().idJob()).isEqualTo("job-geralanding-image-large");
    }

    /** Cria o client imagegeneration apontando para o backend simulado do teste. */
    private ImageGenerationBackendClient newClient() {
        return new ImageGenerationBackendClient(
                mock(FrameworkImageStorageClient.class),
                mock(CreativeImageOptimizer.class),
                WebClient.builder(),
                objectMapper,
                new ImageGenerationWorkerProperties(
                        true,
                        5,
                        server.url("/").toString(),
                        "/api",
                        "gpt-image-2",
                        Duration.ofSeconds(5),
                        3,
                        Duration.ofMillis(300),
                        "worker-test",
                        100,
                        0.01d),
                1024 * 1024);
    }
}
