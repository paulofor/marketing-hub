package com.marketinghub.worker.openai.core.copy;

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

/** Responsabilidade: validar o contrato HTTP do client copy do core OpenAI. */
class CopyBackendClientTest {

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

    /** Deve converter o payload pendente do backend em execução com wireframe e CASE_DATA_BLOCK para o prompt. */
    @Test
    void listPendingShouldBuildCopyPromptDataWithWireframeAndCaseDataBlock() throws Exception {
        Map<String, Object> pendingExecution = Map.of(
                "experimentId", 12,
                "stageCode", "landing-page-copy",
                "jobid", "job-copy-1",
                "executionRequestedAt", "2026-05-30T10:00:00Z",
                "experiment", Map.of(
                        "nicheName", "Produtores digitais",
                        "singlePain", "clientes desmarcam horário",
                        "freeReward", "3 mensagens prontas",
                        "funnelPromise", "Receber as 3 mensagens",
                        "primaryCta", "Receber as 3 mensagens",
                        "campaignObjective", "LEADS",
                        "campaignAngle", "{\"promise\":\"menos esforço para vender\"}",
                        "adCopy", "{\"headline\":\"Venda com IA\"}",
                        "adImageBriefing", "{\"visual\":\"dashboard simples\"}",
                        "landingPageWireframe", "{\"pagina\":{\"corpo\":{\"secoes\":[]}}}"),
                "hypothesis", Map.of("framework", Map.of(
                        "pain", Map.of("main", "perde tempo"),
                        "result", Map.of("main", "vender com clareza"),
                        "mechanism", Map.of("main", "roteiro guiado"),
                        "proof", Map.of("main", "checklist"),
                        "offer", Map.of("main", "template"))));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(List.of(pendingExecution))));
        CopyBackendClient client = newClient();

        List<StageExecution<CopyInput>> result = client.listPending(5);

        assertThat(result).hasSize(1);
        StageExecution<CopyInput> execution = result.getFirst();
        assertThat(execution.idJob()).isEqualTo("job-copy-1");
        assertThat(execution.stageCode()).isEqualTo("landing-page-copy");
        assertThat(execution.aggregateId()).isEqualTo(12L);
        assertThat(execution.input().promptData())
                .containsKeys("landingPageWireframe", "campaignAngle", "adCopy", "adImageBriefing", "singlePain", "freeReward", "funnelPromise", "primaryCta", "campaignObjective", "CASE_DATA_BLOCK");
        assertThat(execution.input().promptData().get("landingPageWireframe").toString())
                .contains("pagina");
        assertThat((String) execution.input().promptData().get("CASE_DATA_BLOCK"))
                .contains("[CASE_DATA_BEGIN]")
                .contains("Produtores digitais")
                .contains("clientes desmarcam horário")
                .contains("3 mensagens prontas")
                .contains("Receber as 3 mensagens")
                .contains("LEADS")
                .contains("menos esforço para vender")
                .contains("roteiro guiado")
                .contains("[CASE_DATA_END]");
    }

    /** Deve enviar prompt, schema e request cru no callback recebe-prompt da etapa copy. */
    @Test
    void markDispatchedShouldSendPromptSchemaAndRawRequestToRecebePrompt() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(202));
        CopyBackendClient client = newClient();
        StageExecution<CopyInput> execution = new StageExecution<>(
                "job-copy-1",
                12L,
                "landing-page-copy",
                "INICIADO",
                Instant.parse("2026-05-30T10:00:00Z"),
                new CopyInput(12L, "landing-page-copy", "job-copy-1", Map.of()));
        OpenAiDispatch dispatch = new OpenAiDispatch(
                "openai-job-copy-1",
                "Prompt copy renderizado",
                "{\"type\":\"object\"}",
                "{\"model\":\"gpt-test\",\"input\":\"Prompt copy renderizado\"}",
                "# Prompt copy markdown bruto",
                Instant.parse("2026-05-30T10:01:00Z"));

        client.markDispatched(execution, dispatch);

        var request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath())
                .isEqualTo("/api/internal/geralanding/copy/stage-executions/job-copy-1/recebe-prompt");
        Map<String, Object> payload = objectMapper.readValue(request.getBody().readUtf8(), new TypeReference<>() {});
        assertThat(payload)
                .containsEntry("prompt", "Prompt copy renderizado")
                .containsEntry("promptMarkdownContent", "# Prompt copy markdown bruto")
                .containsEntry("schemaJson", "{\"type\":\"object\"}")
                .containsEntry("requestBodyJson", "{\"model\":\"gpt-test\",\"input\":\"Prompt copy renderizado\"}")
                .containsEntry("jobidopenai", "openai-job-copy-1");
    }

    /** Deve montar o request da etapa copy usando o modelo dedicado gpt-5.4. */
    @Test
    void copyPromptBuilderShouldUseDedicatedGpt54Model() throws Exception {
        CopyWorkerProperties properties = new CopyWorkerProperties(
                true,
                5,
                "http://backend",
                "/api",
                "prompts/geralanding/landing-page-copy.md",
                "prompts/geralanding/landing-page-copy-schema.json",
                "experiment_pipeline_landing_page_copy",
                "gpt-5.4",
                Duration.ofSeconds(5));
        CopyPromptBuilder builder = new CopyPromptBuilder(objectMapper, properties);
        StageExecution<CopyInput> execution = new StageExecution<>(
                "job-copy-54",
                12L,
                "landing-page-copy",
                "INICIADO",
                Instant.parse("2026-06-05T10:00:00Z"),
                new CopyInput(12L, "landing-page-copy", "job-copy-54", Map.of(
                        "CASE_DATA_BLOCK", Map.of("niche", "nicho"),
                        "WIREFRAME_JSON", Map.of("pagina", Map.of("sections", List.of())))));

        var request = builder.build(execution);
        JsonNode body = objectMapper.readTree(request.requestBodyJson());

        assertThat(request.model()).isEqualTo("gpt-5.4");
        assertThat(body.path("model").asText()).isEqualTo("gpt-5.4");
    }

    /** Cria o client copy apontando para o backend simulado do teste. */
    private CopyBackendClient newClient() {
        return new CopyBackendClient(
                WebClient.builder(),
                new CopyWorkerProperties(
                        true,
                        5,
                        server.url("/").toString(),
                        "/api",
                        "prompts/geralanding/landing-page-copy.md",
                        "prompts/geralanding/landing-page-copy-schema.json",
                        "experiment_pipeline_landing_page_copy",
                        "gpt-5.4",
                        Duration.ofSeconds(5)),
                objectMapper);
    }
}
