package com.marketinghub.worker.openai.core.imageplanning;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.StageExecution;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o contrato HTTP do client image planning do core OpenAI. */
class ImagePlanningBackendClientTest {

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

    /** Deve converter o payload pendente do backend em execução com wireframe para planejar imagens. */
    @Test
    void listPendingShouldBuildImagePlanningPromptDataWithWireframe() throws Exception {
        Map<String, Object> pendingExecution = Map.of(
                "experimentId", 22,
                "stageCode", "landing-page-image-planning",
                "jobid", "job-image-1",
                "executionRequestedAt", "2026-05-31T10:00:00Z",
                "experiment", Map.of(
                        "nicheName", "Produtores digitais",
                        "campaignAngle", "{\"promise\":\"menos esforço para vender\"}",
                        "adCopy", "{\"headline\":\"Venda com IA\"}",
                        "adImageBriefing", "{\"visual\":\"dashboard simples\"}",
                        "landingPageWireframe", "{\"pagina\":{\"corpo\":{\"secoes\":[{\"tag\":\"img\"}]}}}"),
                "hypothesis", Map.of("framework", Map.of(
                        "pain", Map.of("main", "perde tempo"),
                        "result", Map.of("main", "vender com clareza"))));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(List.of(pendingExecution))));
        ImagePlanningBackendClient client = newClient();

        List<StageExecution<ImagePlanningInput>> result = client.listPending(5);

        assertThat(result).hasSize(1);
        StageExecution<ImagePlanningInput> execution = result.getFirst();
        assertThat(execution.idJob()).isEqualTo("job-image-1");
        assertThat(execution.stageCode()).isEqualTo("landing-page-image-planning");
        assertThat(execution.aggregateId()).isEqualTo(22L);
        assertThat(execution.input().promptData())
                .containsKeys("landingPageWireframe", "campaignAngle", "adCopy", "adImageBriefing", "NICHE_NAME");
        assertThat(execution.input().promptData().get("landingPageWireframe").toString())
                .contains("pagina");
        assertThat(execution.input().promptData().get("NICHE_NAME"))
                .isEqualTo("Produtores digitais");
    }

    /** Deve aceitar a variação idJob do contrato pendente sem quebrar a montagem da execução. */
    @Test
    void listPendingShouldAcceptIdJobAliasFromBackend() throws Exception {
        Map<String, Object> pendingExecution = Map.of(
                "experimentId", 22,
                "stageCode", "landing-page-image-planning",
                "idJob", "job-image-alias",
                "executionRequestedAt", "2026-05-31T10:00:00Z",
                "experiment", Map.of("nicheName", "Produtores digitais"),
                "hypothesis", Map.of("framework", Map.of()));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(List.of(pendingExecution))));
        ImagePlanningBackendClient client = newClient();

        List<StageExecution<ImagePlanningInput>> result = client.listPending(5);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().idJob()).isEqualTo("job-image-alias");
    }

    /** Deve ignorar payload pendente incompleto para evitar NullPointerException no contrato interno. */
    @Test
    void listPendingShouldIgnoreIncompletePendingPayload() throws Exception {
        Map<String, Object> pendingExecution = Map.of(
                "experimentId", 22,
                "stageCode", "landing-page-image-planning",
                "executionRequestedAt", "2026-05-31T10:00:00Z",
                "experiment", Map.of("nicheName", "Produtores digitais"),
                "hypothesis", Map.of("framework", Map.of()));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(List.of(pendingExecution))));
        ImagePlanningBackendClient client = newClient();

        List<StageExecution<ImagePlanningInput>> result = client.listPending(5);

        assertThat(result).isEmpty();
    }

    /** Deve enviar prompt, schema e request cru no callback recebe-prompt da etapa image planning. */
    @Test
    void markDispatchedShouldSendPromptSchemaAndRawRequestToRecebePrompt() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(202));
        ImagePlanningBackendClient client = newClient();
        StageExecution<ImagePlanningInput> execution = new StageExecution<>(
                "job-image-1",
                22L,
                "landing-page-image-planning",
                "INICIADO",
                Instant.parse("2026-05-31T10:00:00Z"),
                new ImagePlanningInput(22L, "landing-page-image-planning", "job-image-1", Map.of()));
        OpenAiDispatch dispatch = new OpenAiDispatch(
                "openai-job-image-1",
                "Prompt image planning renderizado",
                "{\"type\":\"object\"}",
                "{\"model\":\"gpt-test\",\"input\":\"Prompt image planning renderizado\"}",
                "# Prompt image planning markdown bruto",
                Instant.parse("2026-05-31T10:01:00Z"));

        client.markDispatched(execution, dispatch);

        var request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath())
                .isEqualTo("/api/internal/geralanding/image-prompts/stage-executions/job-image-1/recebe-prompt");
        Map<String, Object> payload = objectMapper.readValue(request.getBody().readUtf8(), new TypeReference<>() {});
        assertThat(payload)
                .containsEntry("prompt", "Prompt image planning renderizado")
                .containsEntry("promptMarkdownContent", "# Prompt image planning markdown bruto")
                .containsEntry("schemaJson", "{\"type\":\"object\"}")
                .containsEntry("requestBodyJson", "{\"model\":\"gpt-test\",\"input\":\"Prompt image planning renderizado\"}")
                .containsEntry("jobidopenai", "openai-job-image-1");
    }

    /** Deve montar o request da etapa image planning usando o modelo dedicado gpt-5.4. */
    @Test
    void imagePlanningPromptBuilderShouldUseDedicatedGpt54Model() throws Exception {
        ImagePlanningWorkerProperties properties = new ImagePlanningWorkerProperties(
                true,
                5,
                "http://backend",
                "/api",
                "prompts/geralanding/landing-page-image-planning.md",
                "prompts/geralanding/landing-page-image-planning-schema.json",
                "experiment_pipeline_landing_page_image_planning",
                "gpt-5.4",
                Duration.ofSeconds(5));
        ImagePlanningPromptBuilder builder = new ImagePlanningPromptBuilder(objectMapper, properties);
        StageExecution<ImagePlanningInput> execution = new StageExecution<>(
                "job-image-54",
                12L,
                "landing-page-image-planning",
                "INICIADO",
                Instant.parse("2026-06-05T10:00:00Z"),
                new ImagePlanningInput(12L, "landing-page-image-planning", "job-image-54", Map.of(
                        "CASE_DATA_BLOCK", Map.of("niche", "nicho"),
                        "WIREFRAME_JSON", Map.of("pagina", Map.of("sections", List.of())),
                        "COPY_JSON", Map.of("bodySections", List.of()))));

        var request = builder.build(execution);
        JsonNode body = objectMapper.readTree(request.requestBodyJson());

        assertThat(request.model()).isEqualTo("gpt-5.4");
        assertThat(body.path("model").asText()).isEqualTo("gpt-5.4");
    }

    /** Cria o client image planning apontando para o backend simulado do teste. */
    private ImagePlanningBackendClient newClient() {
        return new ImagePlanningBackendClient(
                WebClient.builder(),
                new ImagePlanningWorkerProperties(
                        true,
                        5,
                        server.url("/").toString(),
                        "/api",
                        "prompts/geralanding/landing-page-image-planning.md",
                        "prompts/geralanding/landing-page-image-planning-schema.json",
                        "experiment_pipeline_landing_page_image_planning",
                        "gpt-5.4",
                        Duration.ofSeconds(5)),
                objectMapper);
    }
}
