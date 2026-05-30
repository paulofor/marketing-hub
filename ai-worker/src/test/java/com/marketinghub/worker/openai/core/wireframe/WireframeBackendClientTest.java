package com.marketinghub.worker.openai.core.wireframe;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.OpenAiHttpException;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.StageExecution;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o contrato HTTP do client wireframe do core OpenAI. */
class WireframeBackendClientTest {

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

    /** Deve enviar prompt, schema e request cru no callback recebe-prompt. */
    @Test
    void markDispatchedShouldSendPromptSchemaAndRawRequestToRecebePrompt() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(202));
        WireframeBackendClient client = new WireframeBackendClient(
                WebClient.builder(),
                new WireframeWorkerProperties(
                        true,
                        5,
                        server.url("/").toString(),
                        "/api",
                        "prompts/geralanding/landing-page-wireframe.md",
                        "prompts/geralanding/landing-page-wireframe-schema.json",
                        "experiment_pipeline_landing_page_wireframe",
                        Duration.ofSeconds(5)),
                objectMapper);
        StageExecution<WireframeInput> execution = new StageExecution<>(
                "job-ia-1",
                12L,
                "landing-page-wireframe",
                "INICIADO",
                Instant.parse("2026-05-29T10:00:00Z"),
                new WireframeInput(12L, "landing-page-wireframe", "job-ia-1", Map.of()));
        OpenAiDispatch dispatch = new OpenAiDispatch(
                "openai-job-1",
                "Prompt renderizado",
                "{\"type\":\"object\"}",
                "{\"model\":\"gpt-test\",\"input\":\"Prompt renderizado\"}",
                Instant.parse("2026-05-29T10:01:00Z"));

        client.markDispatched(execution, dispatch);

        var request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath())
                .isEqualTo("/api/internal/geralanding/wireframe/stage-executions/job-ia-1/recebe-prompt");
        Map<String, Object> payload = objectMapper.readValue(request.getBody().readUtf8(), new TypeReference<>() {});
        assertThat(payload)
                .containsEntry("prompt", "Prompt renderizado")
                .containsEntry("schemaJson", "{\"type\":\"object\"}")
                .containsEntry("requestBodyJson", "{\"model\":\"gpt-test\",\"input\":\"Prompt renderizado\"}")
                .containsEntry("jobidopenai", "openai-job-1");
    }

    /** Deve enviar o payload bruto de erro da OpenAI no callback recebe-resposta quando a etapa falhar. */
    @Test
    void markFailedShouldSendOpenAiRawErrorPayloadToRecebeResposta() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(202));
        WireframeBackendClient client = new WireframeBackendClient(
                WebClient.builder(),
                new WireframeWorkerProperties(
                        true,
                        5,
                        server.url("/").toString(),
                        "/api",
                        "prompts/geralanding/landing-page-wireframe.md",
                        "prompts/geralanding/landing-page-wireframe-schema.json",
                        "experiment_pipeline_landing_page_wireframe",
                        Duration.ofSeconds(5)),
                objectMapper);
        StageExecution<WireframeInput> execution = new StageExecution<>(
                "job-ia-1",
                12L,
                "landing-page-wireframe",
                "INICIADO",
                Instant.parse("2026-05-29T10:00:00Z"),
                new WireframeInput(12L, "landing-page-wireframe", "job-ia-1", Map.of()));
        var error = new OpenAiHttpException(
                400,
                "{\n  \"error\": {\n    \"message\": \"Invalid schema for response_format 'experiment_pipeline_landing_page_wireframe': In context=(), 'allOf' is not permitted.\",\n    \"type\": \"invalid_request_error\",\n    \"param\": \"text.format.schema\",\n    \"code\": \"invalid_json_schema\"\n  }\n}",
                new RuntimeException("HTTP 400"));

        client.markFailed(execution, error);

        var request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath())
                .isEqualTo("/api/internal/geralanding/wireframe/stage-executions/job-ia-1/recebe-resposta");
        Map<String, Object> payload = objectMapper.readValue(request.getBody().readUtf8(), new TypeReference<>() {});
        assertThat(payload)
                .containsEntry("experimentId", 12)
                .containsEntry("stageCode", "landing-page-wireframe")
                .containsEntry("modelResponse", null)
                .containsEntry("openAiJobId", null);
        assertThat((String) payload.get("errorMessage"))
                .contains("OpenAI Responses API returned HTTP 400")
                .contains("Invalid schema for response_format")
                .contains("invalid_json_schema");
        assertThat((String) payload.get("errorDetail"))
                .contains("Invalid schema for response_format")
                .contains("'allOf' is not permitted")
                .contains("text.format.schema");
    }

    /** Deve manter o schema de wireframe sem palavras-chave rejeitadas pela OpenAI Structured Outputs. */
    @Test
    void wireframeSchemaShouldNotContainUnsupportedOpenAiStructuredOutputKeywords() throws Exception {
        String schemaJson = new ClassPathResource("prompts/geralanding/landing-page-wireframe-schema.json")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(schemaJson)
                .doesNotContain("\"allOf\"")
                .doesNotContain("\"if\"")
                .doesNotContain("\"then\"")
                .doesNotContain("\"not\"");
    }

}
