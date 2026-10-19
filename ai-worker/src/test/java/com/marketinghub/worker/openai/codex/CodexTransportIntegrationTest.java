package com.marketinghub.worker.openai.codex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.OpenAiChatGptClient;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.core.openai.OpenAiClientProperties;
import com.marketinghub.worker.openai.core.openai.ResponsesApiOpenAiClient;
import com.marketinghub.worker.pipeline.InMemoryArtifactStore;
import com.marketinghub.worker.pipeline.PipelineWorker;
import com.marketinghub.worker.pipeline.gerasalespagev1.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Comprova fila, App Server, contrato, custo e callback reais do GeraSalesPage com provedor local.
 */
class CodexTransportIntegrationTest {
  private static final String SCHEMA =
      "{\"type\":\"object\",\"properties\":{\"ready\":{\"type\":\"boolean\"}},\"required\":[\"ready\"],\"additionalProperties\":false}";
  @TempDir Path directory;
  private final ObjectMapper mapper = new ObjectMapper();
  private final List<JsonNode> callbacks = new ArrayList<>();
  private final AtomicBoolean refuseFirstSuccess = new AtomicBoolean();
  private final AtomicBoolean replayOnly = new AtomicBoolean();
  private MockWebServer backend;
  private String jobId;
  private long experimentId;

  /** Inicia apenas um backend local e isola todas as identidades e arquivos de teste. */
  @BeforeEach
  void prepare() throws Exception {
    backend = new MockWebServer();
    backend.setDispatcher(
        new Dispatcher() {
          /**
           * Simula os contratos oficiais, incluindo indisponibilidade do primeiro callback de
           * sucesso.
           */
          @Override
          public MockResponse dispatch(RecordedRequest request) {
            try {
              String path = request.getPath();
              if (path.endsWith("/pending"))
                return json(
                    mapper.writeValueAsString(
                        List.of(
                            Map.of(
                                "jobid",
                                jobId,
                                "replayOnly",
                                replayOnly.get(),
                                "experimentId",
                                experimentId,
                                "stageCode",
                                "sales-page-offer-brief",
                                "experiment",
                                Map.of("id", experimentId),
                                "previousStageOutputs",
                                Map.of(),
                                "promptTemplate",
                                Map.of(
                                    "model",
                                    "gpt-test",
                                    "schemaName",
                                    "ready",
                                    "schemaJson",
                                    SCHEMA,
                                    "promptMarkdownContent",
                                    "Uma oferta útil e fiel ao produto.")))));
              if (path.equals("/pricing"))
                return json(
                    "[{\"code\":\"gpt-test\",\"priceInputStandard\":10,\"priceOutputStandard\":20,\"priceInputBatch\":1,\"priceOutputBatch\":2}]");
              if (path.endsWith("/recebe-resposta")) {
                JsonNode body = mapper.readTree(request.getBody().readUtf8());
                callbacks.add(body);
                if (!body.path("modelResponse").isNull()
                    && refuseFirstSuccess.compareAndSet(true, false))
                  return new MockResponse().setResponseCode(503);
              }
              return new MockResponse().setResponseCode(204);
            } catch (IOException ex) {
              throw new IllegalStateException("Falha ao simular backend local", ex);
            }
          }
        });
    backend.start();
  }

  /** Encerra o servidor local após cada cenário. */
  @AfterEach
  void finish() throws Exception {
    backend.shutdown();
  }

  /**
   * Percorre o caso original, outra identidade e um caminho previamente válido sem tráfego pago.
   */
  @ParameterizedTest
  @ValueSource(longs = {105, 9106, 99107})
  void shouldCompletePendingThroughCodexAndOfficialCallback(long identity) throws Exception {
    experimentId = identity;
    jobId = "synthetic-job-" + identity;
    var worker = worker("{}");
    assertThat(worker.processPending(1).succeeded()).isEqualTo(1);
    JsonNode callback = callbacks.getFirst();
    assertThat(callback.path("experimentId").asLong()).isEqualTo(identity);
    assertThat(callback.path("modelResponse").asText()).isEqualTo("{\"ready\":true}");
    assertThat(callback.path("costUsd").decimalValue()).isEqualByComparingTo("0.00120000");
    JsonNode receipt = mapper.readTree(callback.path("rawResponse").asText());
    assertThat(
            receipt
                .path("codex_app_server")
                .path("request")
                .path("turn")
                .path("outputSchema")
                .isObject())
        .isTrue();
    assertThat(receipt.path("service_tier").asText()).isEqualTo("default");
    assertThat(inferenceCount()).isEqualTo(1);
  }

  /** Reentrega resultado após falha de callback sem executar uma segunda inferência. */
  @Test
  void shouldReplayReceiptAfterCallbackFailure() throws Exception {
    experimentId = 105;
    jobId = "synthetic-replay-105";
    refuseFirstSuccess.set(true);
    var worker = worker("{}");
    assertThat(worker.processPending(1).failed()).isEqualTo(1);
    assertThat(worker.processPending(1).succeeded()).isEqualTo(1);
    assertThat(inferenceCount()).isEqualTo(1);
    assertThat(callbacks.getFirst().path("rawResponse").asText())
        .isEqualTo(callbacks.getLast().path("rawResponse").asText());
  }

  /** Entrega recibos reais acima de 256 KiB e os recupera sem repetir inferência em dois produtos. */
  @ParameterizedTest
  @ValueSource(longs = {105, 9106})
  void shouldDeliverLargeAuditReceiptAndReplayWithoutInference(long identity) throws Exception {
    experimentId = identity;
    jobId = "synthetic-large-receipt-" + identity;
    var worker = worker("{\"auditPadding\":460000}");
    assertThat(worker.processPending(1).succeeded()).isEqualTo(1);
    replayOnly.set(true);
    assertThat(worker.processPending(1).succeeded()).isEqualTo(1);
    assertThat(inferenceCount()).isEqualTo(1);
    assertThat(callbacks.getFirst().path("rawResponse").asText()).hasSizeGreaterThan(460000);
    assertThat(callbacks.getLast().path("rawResponse").asText())
        .isEqualTo(callbacks.getFirst().path("rawResponse").asText());
    assertThat(callbacks.getFirst().path("costUsd").decimalValue())
        .isEqualByComparingTo("0.00120000");
  }

  /** A fila de recuperação nunca gera quando o recibo está ausente ou pertence a outro job. */
  @Test
  void shouldBlockReplayQueueWithoutExactReceipt() throws Exception {
    experimentId = 105;
    jobId = "synthetic-missing-receipt";
    replayOnly.set(true);
    assertThat(worker("{}").processPending(1).failed()).isEqualTo(1);
    assertThat(callbacks.getFirst().path("errorMessage").asText()).contains("CODEX_RECEIPT_NOT_FOUND");
    assertThat(Files.exists(directory.resolve("session/calls.jsonl"))).isFalse();
  }

  /** Falha de contrato preserva o recibo e bloqueia conclusão, mesmo na reentrega do job. */
  @Test
  void shouldKeepInvalidReceiptWithoutPaidRetry() throws Exception {
    experimentId = 105;
    jobId = "synthetic-invalid-105";
    var worker = worker("{\"text\":\"{\\\"ready\\\":\\\"talvez\\\"}\"}");
    assertThat(worker.processPending(1).failed()).isEqualTo(1);
    assertThat(worker.processPending(1).failed()).isEqualTo(1);
    assertThat(inferenceCount()).isEqualTo(1);
    assertThat(callbacks.getFirst().path("errorMessage").asText())
        .contains("viola schema")
        .contains("recibo persistido");
  }

  /** Resposta inválida grande fica no recibo, sem exceder o campo operacional de erro do backend. */
  @Test
  void shouldKeepLargeInvalidOutputOutOfOperationalErrorMessage() throws Exception {
    experimentId = 105;
    jobId = "synthetic-large-invalid-105";
    String invalid = mapper.writeValueAsString(Map.of("ready", true, "unexpected", "x".repeat(70000)));
    assertThat(worker(mapper.writeValueAsString(Map.of("text", invalid))).processPending(1).failed()).isEqualTo(1);
    assertThat(callbacks.getFirst().path("errorMessage").asText()).hasSizeLessThan(2000);
    try (var receipts = Files.list(directory.resolve("receipts"))) {
      Path receipt = receipts.filter(path -> path.toString().endsWith(".json")).findFirst().orElseThrow();
      assertThat(Files.size(receipt)).isGreaterThan(70000);
    }
    assertThat(inferenceCount()).isEqualTo(1);
  }

  /** Limita o recibo sem remover a proteção de memória nem executar integração de rede. */
  @Test
  void shouldKeepFiniteReceiptDecoderLimit() {
    CodexAppServerTransport transport = mock(CodexAppServerTransport.class);
    when(transport.execute(any())).thenReturn("{\"audit\":\"" + "x".repeat(16 * 1024 * 1024) + "\"}");
    var client = WebClient.builder()
        .filter(new CodexResponsesFilter(backend.url("/v1").toString(), transport, mapper)).build();
    assertThatThrownBy(() -> client.post().uri(backend.url("/v1/responses").toString())
        .bodyValue(Map.of("input", "test")).retrieve().bodyToMono(Map.class).block())
        .hasRootCauseInstanceOf(org.springframework.core.io.buffer.DataBufferLimitException.class);
    assertThat(backend.getRequestCount()).isZero();
  }

  /** Uso ausente continua desconhecido em vez de produzir custo zero fictício. */
  @Test
  void shouldPreserveUnknownUsage() throws Exception {
    experimentId = 105;
    jobId = "synthetic-unknown-105";
    assertThat(worker("{\"missingUsage\":true}").processPending(1).succeeded()).isEqualTo(1);
    assertThat(callbacks.getFirst().path("costUsd").isNull()).isTrue();
    assertThat(callbacks.getFirst().path("inputTokens").isNull()).isTrue();
  }

  /** Falta de sessão não inicia turno e é reportada pelo callback oficial de falha. */
  @Test
  void shouldBlockMissingAuthWithoutFallback() throws Exception {
    experimentId = 105;
    jobId = "synthetic-auth-105";
    assertThat(worker("{\"auth\":false}").processPending(1).failed()).isEqualTo(1);
    assertThat(inferenceCount()).isZero();
    assertThat(callbacks.getFirst().path("errorMessage").asText()).contains("CODEX_AUTH_REQUIRED");
  }

  /**
   * Requisições de imagem e endpoints do backend conservam a integração explicitamente selecionada.
   */
  @Test
  void shouldPreserveBackendAndSpecializedImageRequests() {
    CodexAppServerTransport transport = mock(CodexAppServerTransport.class);
    var client =
        WebClient.builder()
            .filter(new CodexResponsesFilter(backend.url("/v1").toString(), transport, mapper))
            .build();
    client
        .post()
        .uri(backend.url("/api/callback").toString())
        .bodyValue(Map.of("ready", true))
        .retrieve()
        .toBodilessEntity()
        .block();
    client
        .post()
        .uri(backend.url("/v1/responses").toString())
        .bodyValue(Map.of("tools", List.of(Map.of("type", "image_generation"))))
        .retrieve()
        .toBodilessEntity()
        .block();
    verifyNoInteractions(transport);
    assertThat(backend.getRequestCount()).isEqualTo(2);
  }

  /** O cliente legado de enriquecimento também respeita a configuração comum, sem HTTP direto. */
  @Test
  void shouldRouteLegacyTextClientThroughCodex() {
    CodexAppServerTransport transport = mock(CodexAppServerTransport.class);
    when(transport.execute(any()))
        .thenReturn("{\"id\":\"codex-test\",\"status\":\"completed\",\"output_text\":\"ok\"}");
    var builder =
        WebClient.builder()
            .filter(new CodexResponsesFilter(backend.url("/v1").toString(), transport, mapper));
    var legacy =
        new OpenAiChatGptClient(
            builder,
            backend.url("/v1").toString(),
            "codex-app-server",
            null,
            "gpt-test",
            null,
            null,
            mock(AiGenerationRecorder.class));
    Object response =
        ReflectionTestUtils.invokeMethod(
            legacy, "executeResponseRequest", Map.of("model", "gpt-test", "input", "Teste"));
    assertThat(response).isNotNull();
    assertThat(backend.getRequestCount()).isZero();
  }

  /** A normalização defensiva da URL não permite escapar para o endpoint pago oficial. */
  @Test
  void shouldInterceptCanonicalEndpointEvenWhenAClientNormalizesItsBaseUrl() {
    CodexAppServerTransport transport = mock(CodexAppServerTransport.class);
    when(transport.execute(any())).thenReturn("{\"status\":\"completed\",\"output_text\":\"ok\"}");
    var client =
        WebClient.builder()
            .filter(new CodexResponsesFilter(backend.url("/v1").toString(), transport, mapper))
            .build();
    String result =
        client
            .post()
            .uri("https://api.openai.com/v1/responses")
            .bodyValue(Map.of("model", "gpt-test", "input", "Teste"))
            .retrieve()
            .bodyToMono(String.class)
            .block();
    assertThat(result).contains("completed");
    assertThat(backend.getRequestCount()).isZero();
  }

  /** Constrói o worker e os adapters produtivos com somente dependências externas simuladas. */
  private PipelineWorker<GeraSalesPageInput, GeraSalesPageOutput> worker(String fixture)
      throws Exception {
    Path home = directory.resolve("session");
    Files.createDirectories(home);
    Files.writeString(home.resolve("fixture.json"), fixture);
    Path bridge = directory.resolve("bridge.mjs");
    Path moduleRoot = Path.of(System.getProperty("basedir", ".")).toAbsolutePath();
    String module = moduleRoot.resolve("codex/responses.mjs").toUri().toString();
    String fake = moduleRoot.resolve("codex/fixtures/app-server.mjs").toString();
    Files.writeString(
        bridge,
        "import {execute, validateResponse} from '"
            + module
            + "';\n"
            + "let raw='';for await(const chunk of process.stdin)raw+=chunk;const body=JSON.parse(raw);\n"
            + "try { const response=await execute(body,{home:"
            + mapper.writeValueAsString(home.toString())
            + ",workspace:"
            + mapper.writeValueAsString(home.toString())
            + ",cacheDirectory:"
            + mapper.writeValueAsString(directory.resolve("receipts").toString())
            + ",command:"
            + mapper.writeValueAsString(fake)
            + ",noLock:true});process.stdout.write(JSON.stringify(response));validateResponse(body,response); }"
            + "catch(error){process.stderr.write(error.message);process.exitCode=1;}\n");
    var transport =
        new CodexAppServerTransport("node", bridge.toString(), Duration.ofSeconds(10), mapper);
    var builder =
        WebClient.builder()
            .filter(new CodexResponsesFilter(backend.url("/v1").toString(), transport, mapper));
    var properties =
        new OpenAiClientProperties(
            null,
            null,
            backend.url("/v1").toString(),
            "gpt-test",
            Duration.ofSeconds(10),
            backend.url("/pricing").toString(),
            true,
            "codex-app-server");
    var client = new ResponsesApiOpenAiClient(builder, mapper, properties);
    var workerProperties =
        new GeraSalesPageWorkerProperties(
            true,
            1,
            backend.url("/").toString(),
            "/api",
            List.of("sales-page-offer-brief"),
            Duration.ofSeconds(10),
            "flex");
    var port = new GeraSalesPageBackendClient(builder, workerProperties, mapper);
    var processor =
        new GeraSalesPageProcessor(
            mapper, client, new GeraSalesPageResponseValidator(mapper), port, "flex");
    return new PipelineWorker<>(port, processor, new InMemoryArtifactStore());
  }

  /** Conta turnos no provedor simulado, independente da quantidade de callbacks e reentregas. */
  private long inferenceCount() throws Exception {
    try (var lines = Files.lines(directory.resolve("session/calls.jsonl"))) {
      return lines.filter(line -> line.contains("\"method\":\"turn/start\"")).count();
    }
  }

  /** Entrega JSON canônico pelo backend local de teste. */
  private MockResponse json(String body) {
    return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
  }
}
