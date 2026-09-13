package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

/**
 * Responsabilidade: homologar produção, storage, callback e limpeza usando integrações HTTP locais.
 */
class IrisCreativeMaterializerTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private HttpServer server;
  private byte[] source;
  private String sourceHash;
  private boolean rejectUpload;
  private boolean mobileAvailable;
  private boolean invalidMobileHash;
  private String mode = "LEARNING_CYCLE_PRIVATE";
  private final AtomicReference<byte[]> saved = new AtomicReference<>();
  private IrisCreativeMaterializer materializer;

  /** Simula exclusivamente contratos internos com IDs e arquivos segregados de produção. */
  @BeforeEach
  void setup() throws Exception {
    source = ProofCardRendererTest.source();
    sourceHash = IrisCreativeMaterializer.sha(source);
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/internal/agent-tasks/communication-director/stage-executions/910403",
        exchange -> {
          byte[] response;
          String contentType = "application/json";
          int status = 200;
          String path = exchange.getRequestURI().getPath();
          if (path.endsWith("/visual-inputs")) {
            response =
                ("[{\"sourceTaskId\":910395,\"prototypeVersion\":\"sandbox-v12\",\"evidence\":{\"id\":910118,\"deviceProfile\":\"DESKTOP_1440\",\"sha256\":\""
                        + sourceHash
                        + "\",\"sourceUrl\":\"https://sandbox.example/prototype\",\"finalUrl\":\"https://sandbox.example/prototype\"}}]")
                    .getBytes(StandardCharsets.UTF_8);
            if (mobileAvailable) {
              var inputs = (com.fasterxml.jackson.databind.node.ArrayNode) json.readTree(response);
              var mobile = inputs.get(0).deepCopy();
              ((ObjectNode) mobile.path("evidence"))
                  .put("id", 910119)
                  .put("deviceProfile", "IPHONE_15_PRO")
                  .put("viewportWidth", 393)
                  .put("sha256", invalidMobileHash ? "0".repeat(64) : sourceHash);
              inputs.add(mobile);
              var extra = mobile.deepCopy();
              ((ObjectNode) extra.path("evidence"))
                  .put("id", 910120)
                  .put("deviceProfile", "PIXEL_7");
              inputs.add(extra);
              response = json.writeValueAsBytes(inputs);
            }
          } else if (path.endsWith("/910395/910118/content")
              || path.endsWith("/910395/910119/content")) {
            response = source;
            contentType = "image/png";
          } else if (path.endsWith("/visual-evidence")
              && "POST".equals(exchange.getRequestMethod())) {
            byte[] body = exchange.getRequestBody().readAllBytes();
            int start = indexOf(body, new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
            int end = indexOf(body, new byte[] {73, 69, 78, 68}) + 8;
            if (start < 0 || end <= start)
              throw new java.io.IOException("Multipart não contém PNG.");
            byte[] png = Arrays.copyOfRange(body, start, end);
            saved.set(png);
            status = rejectUpload ? 503 : 200;
            response =
                ("{\"id\":910130,\"sha256\":\""
                        + IrisCreativeMaterializer.sha(png)
                        + "\",\"contentUrl\":\"/api/agent-tasks/910403/visual-evidence/910130/content\"}")
                    .getBytes(StandardCharsets.UTF_8);
          } else {
            status = 404;
            response = "{}".getBytes(StandardCharsets.UTF_8);
          }
          exchange.getResponseHeaders().set("Content-Type", contentType);
          exchange.sendResponseHeaders(status, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    var properties = properties();
    materializer = new IrisCreativeMaterializer(properties, new ProofCardRenderer(), json);
  }

  /** Uma captura mobile aprovada chega ao modelo e pode originar a peça sem perder o desktop. */
  @Test
  void suppliesMobileAndDesktopAndRendersSelectedMobile() throws Exception {
    mobileAvailable = true;
    var task = task();
    try (var prepared = materializer.prepare(task)) {
      assertThat(prepared.inputs()).hasSize(2);
      assertThat(prepared.inputs().getFirst().metadata().path("evidence").path("id").asLong())
          .isEqualTo(910119L);
      assertThat(json.valueToTree(task.get("approvedVisualInputs")).size()).isEqualTo(2);
      var output = result();
      ((ObjectNode) output.path("functionalOutput").path("staticAssets").get(0).path("renderSpec"))
          .put("sourceArtifactId", 910119L);
      var rendered = materializer.materialize(task, output, prepared);
      assertThat(
              rendered
                  .path("functionalOutput")
                  .path("renderedAssets")
                  .get(0)
                  .path("sourceArtifactId")
                  .asLong())
          .isEqualTo(910119L);
    }
  }

  /** Uma fonte mobile com hash divergente bloqueia antes de solicitar modelo ou salvar peça. */
  @Test
  void refusesCorruptedMobileProof() {
    mobileAvailable = true;
    invalidMobileHash = true;
    assertThatThrownBy(() -> materializer.prepare(task())).hasMessageContaining("Hash da prova");
    assertThat(saved.get()).isNull();
  }

  /** Correção sem mudança de pixels é recusada antes do upload e do parecer seguinte. */
  @Test
  void refusesIdenticalRejectedImage() throws Exception {
    var task = task();
    var output = result();
    String hash =
        IrisCreativeMaterializer.sha(
            new ProofCardRenderer()
                .render(
                    output.path("functionalOutput").path("staticAssets").get(0).path("renderSpec"),
                    source,
                    true));
    var context = (ObjectNode) json.readTree(String.valueOf(task.get("processContextJson")));
    context
        .putArray("blockedActivities")
        .addObject()
        .put("taskId", 910413L)
        .putObject("result")
        .put("decision", "ADJUST")
        .putArray("renderedAssetAudit")
        .addObject()
        .put("sha256", hash);
    task.put("processContextJson", context.toString());
    try (var prepared = materializer.prepare(task)) {
      assertThatThrownBy(() -> materializer.materialize(task, output, prepared))
          .hasMessageContaining("repetiu a imagem reprovada");
      assertThat(saved.get()).isNull();
    }
  }

  /** Encerra o backend simulado sem deixar portas ou tarefas locais em execução. */
  @AfterEach
  void cleanup() {
    if (server != null) server.stop(0);
  }

  /**
   * Executa os dois contextos privados, modelo simulado, pixels reais, upload e callback auditável.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"LEARNING_CYCLE_PRIVATE", "PRODUCT_PRIVATE"})
  void completesOnlyAfterPersistingImageAndPreservesRawResponse(String privateMode)
      throws Exception {
    mode = privateMode;
    var backend = mock(CommunicationAgentBackendClient.class);
    var runner = mock(CommunicationAgentCodexRunner.class);
    var control = mock(AutomaticExecutionControl.class);
    when(control.allowsAutomaticExecution()).thenReturn(true);
    when(backend.claim(anyString(), anyString())).thenReturn(task());
    var result = result();
    String raw = result.toPrettyString();
    when(runner.run(anyMap(), anyList()))
        .thenAnswer(
            invocation -> {
              java.util.List<java.nio.file.Path> files = invocation.getArgument(1);
              assertThat(files).hasSize(1);
              assertThat(files.getFirst()).exists();
              return new CommunicationAgentCodexRunner.Execution(
                  result,
                  raw,
                  "prompt exato",
                  "núcleo",
                  "atividade",
                  CommunicationAgentCodexRunner.TokenUsage.empty());
            });
    new CommunicationAgentTaskConsumer(backend, runner, properties(), control, json, materializer)
        .processOne();
    var payload = ArgumentCaptor.forClass(Map.class);
    verify(backend).complete(eq(910403L), payload.capture());
    verify(backend, never()).fail(anyLong(), anyMap());
    var functional = json.readTree(String.valueOf(payload.getValue().get("resultJson")));
    var render = functional.path("functionalOutput").path("renderedAssets").get(0);
    assertThat(render.path("artifactId").asLong()).isEqualTo(910130L);
    assertThat(render.path("sha256").asText()).isEqualTo(IrisCreativeMaterializer.sha(saved.get()));
    assertThat(render.path("sourceSha256").asText()).isEqualTo(sourceHash);
    assertThat(render.path("privateValidation").asBoolean()).isTrue();
    assertThat(
            json.readTree(String.valueOf(payload.getValue().get("evidenceJson")))
                .path("rawModelResponse")
                .asText())
        .isEqualTo(raw);
  }

  /** Falha de storage preserva o parecer e bloqueia a atividade sem enviar sucesso. */
  @Test
  void storageFailureCannotCompleteTask() throws Exception {
    rejectUpload = true;
    var backend = mock(CommunicationAgentBackendClient.class);
    var runner = mock(CommunicationAgentCodexRunner.class);
    var control = mock(AutomaticExecutionControl.class);
    when(control.allowsAutomaticExecution()).thenReturn(true);
    when(backend.claim(anyString(), anyString())).thenReturn(task());
    var result = result();
    when(runner.run(anyMap(), anyList()))
        .thenReturn(
            new CommunicationAgentCodexRunner.Execution(
                result,
                result.toString(),
                "prompt",
                "núcleo",
                "atividade",
                CommunicationAgentCodexRunner.TokenUsage.empty()));
    new CommunicationAgentTaskConsumer(backend, runner, properties(), control, json, materializer)
        .processOne();
    verify(backend, never()).complete(anyLong(), anyMap());
    var payload = ArgumentCaptor.forClass(Map.class);
    verify(backend).fail(eq(910403L), payload.capture());
    assertThat(json.readTree(String.valueOf(payload.getValue().get("resultJson"))))
        .isEqualTo(result);
  }

  /** Monta o contrato do modelo sem inserir artefato ou hash de saída fictícios. */
  private ObjectNode result() throws Exception {
    var result =
        json.createObjectNode()
            .put("executionStatus", "COMPLETED")
            .put("sourceReference", reference());
    var spec = ProofCardRendererTest.spec().put("sourceSha256", sourceHash);
    result
        .putObject("functionalOutput")
        .putArray("staticAssets")
        .addObject()
        .set("renderSpec", spec);
    return result;
  }

  /** Configura uma tarefa privada de teste reservada, sem credenciais ou operação comercial. */
  private Map<String, Object> task() {
    return new HashMap<>(
        Map.of(
            "taskId",
            910403L,
            "processCode",
            "creative-production-approval",
            "activityId",
            "nonAudiovisual",
            "sourceReference",
            reference(),
            "processContextJson",
            "{\"communicationMaterializationContext\":{\"mode\":\""
                + mode
                + "\",\"privatePrototypeAcceptance\":{\"prototypeVersion\":\"sandbox-v12\"}}}"));
  }

  /** Preserva a origem privada e impede que a simulação do produto fabrique um experimento. */
  private String reference() {
    return "PRODUCT_PRIVATE".equals(mode)
        ? "product:91004@agent-validation-v1"
        : "experiment:91092";
  }

  /** Configura apenas o servidor efêmero da matriz local. */
  private CommunicationAgentProperties properties() {
    var properties = new CommunicationAgentProperties();
    properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
    properties.setReasoningEffort("high");
    return properties;
  }

  /** Localiza marcadores binários sem converter pixels para texto. */
  private static int indexOf(byte[] bytes, byte[] sequence) {
    outer:
    for (int i = 0; i <= bytes.length - sequence.length; i++) {
      for (int j = 0; j < sequence.length; j++) if (bytes[i + j] != sequence[j]) continue outer;
      return i;
    }
    return -1;
  }
}
