package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

/** Responsabilidade: comprovar kit real, captura, prompt e callback sem interpretação paga. */
@EnabledIfEnvironmentVariable(named = "KIT_SCENARIO_LOCAL", matches = "true")
class PrivateKitScenarioIntegrationTest {
  @TempDir Path directory;
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  /**
   * Usa os dois contextos reais da matriz local, evitando identidades comerciais fixas no teste.
   */
  static Stream<Arguments> cases() {
    return Stream.of("KIT_LOCAL_INPUT", "KIT_OTHER_LOCAL_INPUT")
        .flatMap(
            key ->
                Stream.of("ADHERENT", "RECOVERY", "SAFETY")
                    .map(s -> Arguments.of(System.getenv(key), s)));
  }

  /** Atravessa a fila e o browser reais; simula somente armazenamento de captura e modelo. */
  @ParameterizedTest
  @MethodSource("cases")
  void deliversEachIndependentScenarioToValidatedCallback(String inputPath, String scenario)
      throws Exception {
    JsonNode input = json.readTree(Path.of(inputPath).toFile());
    String activity =
        switch (scenario) {
          case "ADHERENT" -> "psiqueAdherent";
          case "RECOVERY" -> "psiqueRecovery";
          default -> "psiqueSafety";
        };
    long taskId =
        input.path("productId").asLong() * 100
            + List.of("ADHERENT", "RECOVERY", "SAFETY").indexOf(scenario);
    long evidenceId = taskId * 10;
    var target = json.createObjectNode();
    for (String field : List.of("productId", "productSlug")) target.set(field, input.path(field));
    target.put(
        "experimentId",
        Long.parseLong(input.path("sourceReference").asText().substring("experiment:".length())));
    target.put("experienceVersion", input.path("prototypeVersion").asText());
    target.put("publicUrl", input.path("sourceUrl").asText());
    var context = target.putObject("pdeContext");
    context
        .putObject("lineage")
        .put("productId", input.path("productId").asLong())
        .put("learningCycleId", input.path("cycleId").asLong())
        .put("experimentId", target.path("experimentId").asLong());
    context
        .putObject("privatePrototypeAcceptance")
        .put("runtimeKind", "DETERMINISTIC_PRIVATE_KIT_V1")
        .put("profileCode", input.path("profileCode").asText())
        .put("prototypeVersion", input.path("prototypeVersion").asText())
        .put("packageContractVersion", "PDE_PRIVATE_KIT_PACKAGE_V2");
    var task =
        new HashMap<String, Object>(
            Map.of(
                "taskId",
                taskId,
                "processCode",
                "pde-construction-approval",
                "processVersion",
                11,
                "activityId",
                activity,
                "sourceReference",
                input.path("sourceReference").asText(),
                "taskTarget",
                json.convertValue(target, Object.class)));
    var callback = new AtomicReference<JsonNode>();
    var failure = new AtomicReference<String>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/internal/agent-tasks/",
        exchange -> {
          byte[] body = exchange.getRequestBody().readAllBytes();
          String path = exchange.getRequestURI().getPath();
          Object response = Map.of();
          if (path.endsWith("/pending"))
            response =
                exchange.getRequestURI().getQuery().contains("activityId=" + activity)
                    ? List.of(task)
                    : List.of();
          else if (path.endsWith("/result")) callback.set(json.readTree(body));
          else if (path.endsWith("/failure")) failure.set(new String(body, StandardCharsets.UTF_8));
          byte[] data = json.writeValueAsBytes(response);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, data.length);
          exchange.getResponseBody().write(data);
          exchange.close();
        });
    server.start();
    var upload = mock(BpmVisualEvidenceBackendClient.class);
    when(upload.uploadArtifacts(eq(taskId), anyList()))
        .thenAnswer(
            invocation -> {
              List<BpmVisualEvidenceRunner.VisualArtifact> artifacts = invocation.getArgument(1);
              var receipts = new ArrayList<BpmVisualEvidenceBackendClient.UploadedVisualEvidence>();
              for (var a : artifacts) {
                byte[] pixels = Files.readAllBytes(Path.of(a.localPath()));
                receipts.add(
                    new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
                        evidenceId,
                        a.captureSessionId(),
                        a.evidenceKey(),
                        a.evidenceType(),
                        "Prova privada sintética",
                        a.deviceProfile(),
                        a.pageNumber(),
                        a.foldNumber(),
                        a.viewportWidth(),
                        a.viewportHeight(),
                        a.pageHeightPx(),
                        a.scrollY(),
                        a.sourceUrl(),
                        a.finalUrl(),
                        "/local-evidence/" + evidenceId,
                        (long) pixels.length,
                        HexFormat.of()
                            .formatHex(MessageDigest.getInstance("SHA-256").digest(pixels)),
                        a.capturedAt(),
                        a.localPath()));
              }
              return receipts;
            });
    var anonymous = mock(BpmVisualEvidenceRunner.class);
    var harness =
        new PdeAgentValidationHarnessRunner(
            json,
            "node",
            Path.of("src/main/resources/browser/pde-agent-validation-harness.mjs")
                .toAbsolutePath()
                .toString(),
            "kit-local-internal-only",
            true);
    Path model =
        BpmScenarioModelFixture.create(
            directory, json, target, input.path("sourceReference").asText(), scenario, evidenceId);
    try {
      assertThat(new ProcessBuilder("bash", "-n", model.toString()).start().waitFor()).isZero();
      assertThat(new ProcessBuilder("shellcheck", model.toString()).start().waitFor()).isZero();
      new CustomerBpmTaskConsumer(
              "http://127.0.0.1:" + server.getAddress().getPort(),
              model.toString(),
              "local-test-double",
              "max",
              directory.toString(),
              "/absent-global-evidence",
              json,
              anonymous,
              upload,
              harness,
              new CodexProcessSupervisor(
                  Duration.ofSeconds(30), Duration.ofMinutes(1), Duration.ofMillis(100)))
          .processOne();
      assertThat(failure.get()).as("Callback de falha local").isNull();
      assertThat(callback.get()).isNotNull();
      verifyNoInteractions(anonymous);
      verify(upload).uploadArtifacts(eq(taskId), anyList());
      var result = json.readTree(callback.get().path("resultJson").asText());
      assertThat(result.path("sourceReference").asText())
          .isEqualTo(input.path("sourceReference").asText());
      assertThat(result.path("productId").asLong()).isEqualTo(input.path("productId").asLong());
      assertThat(result.path("scenarioCode").asText()).isEqualTo(scenario);
      String prompt = Files.readString(directory.resolve("prompt.txt"));
      assertThat(prompt)
          .contains(
              "screenshotEvidenceIds", String.valueOf(evidenceId), "PDE_PRIVATE_KIT_PACKAGE_V2")
          .doesNotContain("kit-local-internal-only", "screenshotEvidenceIds\":[]");
      var frozen =
          json.readTree(
              prompt
                  .lines()
                  .filter(
                      line -> line.startsWith("{") && line.contains("\"agentScenarioExecution\""))
                  .findFirst()
                  .orElseThrow());
      var scenarioEvidence = frozen.path("agentScenarioExecution").path("scenarios");
      assertThat(result.path("visualAudit").path("captureSessionId"))
          .isEqualTo(frozen.path("visualCapture").path("captureSessionId"));
      assertThat(scenarioEvidence).hasSize(1);
      assertThat(scenarioEvidence.get(0).path("screenshotEvidenceIds").get(0).asLong())
          .isEqualTo(evidenceId);
      assertThat(scenarioEvidence.toString()).doesNotContain("screenshotEvidenceKeys", "localPath");
      String results = System.getenv("KIT_SCENARIO_RESULTS");
      if (results != null) {
        Path output = Path.of(results).resolve(input.path("profileCode").asText());
        Files.createDirectories(output);
        Files.writeString(output.resolve(scenario + ".json"), result.toPrettyString());
      }
    } finally {
      server.stop(0);
    }
  }
}
