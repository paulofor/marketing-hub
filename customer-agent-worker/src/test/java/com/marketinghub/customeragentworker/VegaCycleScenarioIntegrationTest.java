package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: testar a fila até o callback de Psique com navegador, Vega e MySQL locais. */
@EnabledIfEnvironmentVariable(named = "VEGA_SCENARIO_LOCAL", matches = "true")
class VegaCycleScenarioIntegrationTest {
  @TempDir Path directory;
  private final long cycleId =
      Long.parseLong(System.getenv().getOrDefault("VEGA_LOCAL_CYCLE_ID", "91002"));
  private final long experimentId =
      Long.parseLong(System.getenv().getOrDefault("VEGA_LOCAL_EXPERIMENT_ID", "91092"));
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  /** Executa cada cenário no produto real local; somente fila, upload e modelo são simulados. */
  @ParameterizedTest
  @ValueSource(strings = {"ADHERENT", "RECOVERY", "SAFETY"})
  void consumesCycleWithActualSessionAndReturnsValidatedScenario(String scenario) throws Exception {
    String activity =
        switch (scenario) {
          case "ADHERENT" -> "psiqueAdherent";
          case "RECOVERY" -> "psiqueRecovery";
          default -> "psiqueSafety";
        };
    var target =
        Map.of(
            "productId",
            91004L,
            "experimentId",
            experimentId,
            "productSlug",
            "metodo-musa-7-dias",
            "experienceVersion",
            System.getenv()
                .getOrDefault("VEGA_TEST_VERSION", "musa-pde-entry-v11-primeiro-ajuste-aplicavel"),
            "publicUrl",
            System.getenv()
                .getOrDefault("VEGA_SCENARIO_LOCAL_URL", "http://127.0.0.1:18083/vega-private"),
            "pdeContext",
            Map.of(
                "lineage",
                Map.of(
                    "productId",
                    91004L,
                    "experimentId",
                    experimentId,
                    "learningCycleId",
                    cycleId)));
    var task =
        new HashMap<String, Object>(
            Map.of(
                "taskId",
                910384L,
                "processCode",
                "pde-construction-approval",
                "processVersion",
                8,
                "activityId",
                activity,
                "sourceReference",
                "experiment:" + experimentId,
                "taskTarget",
                target));
    var callback = new AtomicReference<JsonNode>();
    var failure = new AtomicReference<String>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/internal/agent-tasks/",
        exchange -> {
          byte[] body = exchange.getRequestBody().readAllBytes();
          String path = exchange.getRequestURI().getPath();
          Object response = Map.of();
          if (path.endsWith("/pending")) {
            response =
                exchange.getRequestURI().getQuery().contains("activityId=" + activity)
                    ? List.of(task)
                    : List.of();
          } else if (path.endsWith("/result")) callback.set(json.readTree(body));
          else if (path.endsWith("/failure")) failure.set(new String(body, StandardCharsets.UTF_8));
          byte[] data = json.writeValueAsBytes(response);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, data.length);
          exchange.getResponseBody().write(data);
          exchange.close();
        });
    server.start();
    String backend = "http://127.0.0.1:" + server.getAddress().getPort();
    var upload = mock(BpmVisualEvidenceBackendClient.class);
    when(upload.uploadArtifacts(eq(910384L), anyList()))
        .thenAnswer(
            invocation -> {
              List<BpmVisualEvidenceRunner.VisualArtifact> artifacts = invocation.getArgument(1);
              return artifacts.stream()
                  .map(
                      a ->
                          new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
                              910901L,
                              a.captureSessionId(),
                              a.evidenceKey(),
                              a.evidenceType(),
                              "Captura sintética local",
                              a.deviceProfile(),
                              a.pageNumber(),
                              a.foldNumber(),
                              a.viewportWidth(),
                              a.viewportHeight(),
                              a.pageHeightPx(),
                              a.scrollY(),
                              a.sourceUrl(),
                              a.finalUrl(),
                              "/api/agent-tasks/910384/visual-evidence/910901/content",
                              8L,
                              "a".repeat(64),
                              a.capturedAt(),
                              a.localPath()))
                  .toList();
            });
    var anonymous = mock(BpmVisualEvidenceRunner.class);
    var harness =
        new PdeAgentValidationHarnessRunner(
            json,
            "node",
            Path.of("src/main/resources/browser/pde-agent-validation-harness.mjs")
                .toAbsolutePath()
                .toString(),
            "vega-local-internal-only",
            true);
    Path model = fakeModel(scenario);
    try {
      new CustomerBpmTaskConsumer(
              backend,
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
      assertThat(failure.get()).as("Falha do consumidor local").isNull();
      assertThat(callback.get()).isNotNull();
      verifyNoInteractions(anonymous);
      verify(upload).uploadArtifacts(eq(910384L), anyList());
      JsonNode result = json.readTree(callback.get().path("resultJson").asText());
      assertThat(result.path("sourceReference").asText()).isEqualTo("experiment:" + experimentId);
      assertThat(result.path("scenarioCode").asText()).isEqualTo(scenario);
      String flowOutput = System.getenv("VEGA_GATE_FLOW_ARTIFACTS");
      if (flowOutput != null) {
        Files.createDirectories(Path.of(flowOutput));
        Files.writeString(Path.of(flowOutput, scenario + ".json"), result.toPrettyString());
      }
      String prompt = Files.readString(directory.resolve("prompt.txt"));
      assertThat(prompt)
          .contains(
              "revisão sintética de cenário PDE v5",
              "screenshotEvidenceIds",
              "910901",
              "EXPERIENCE_STARTED",
              "AGENT_VALIDATION")
          .doesNotContain(
              "versionedExperienceEvidence",
              "revisão humana da validação privada",
              "vega-local-internal-only");
      if (!"SAFETY".equals(scenario))
        assertThat(prompt).contains("cardId", "READY_RESULT_USED", "CHECKOUT_STARTED");
      assertThat(prompt).doesNotContain("screenshotEvidenceIds\":[]");
    } finally {
      server.stop(0);
    }
  }

  /** Reutiliza o modelo simulado comum sem alterar o contrato local de Vega. */
  private Path fakeModel(String scenario) throws Exception {
    var target =
        json.createObjectNode()
            .put("productId", 91004L)
            .put("productSlug", "metodo-musa-7-dias")
            .put(
                "experienceVersion",
                System.getenv()
                    .getOrDefault(
                        "VEGA_TEST_VERSION", "musa-pde-entry-v11-primeiro-ajuste-aplicavel"));
    return BpmScenarioModelFixture.create(
        directory, json, target, "experiment:" + experimentId, scenario, 910901L);
  }
}
