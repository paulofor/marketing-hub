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
            91092L,
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
                Map.of("productId", 91004L, "experimentId", 91092L, "learningCycleId", 91002L)));
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
                "experiment:91092",
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
      assertThat(result.path("sourceReference").asText()).isEqualTo("experiment:91092");
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

  /** Simula apenas a interpretação do modelo; o consumidor e o harness mantêm todos os gates. */
  private Path fakeModel(String scenario) throws Exception {
    var result = json.createObjectNode();
    result.put("contractVersion", "PDE_PSIQUE_AGENT_SCENARIO_V1");
    result.put("decision", "APPROVED");
    result.put("scenarioCode", scenario);
    result.put("sourceReference", "experiment:91092");
    result.put("productId", 91004L);
    result.put("productSlug", "metodo-musa-7-dias");
    result.put(
        "prototypeVersion",
        System.getenv()
            .getOrDefault("VEGA_TEST_VERSION", "musa-pde-entry-v11-primeiro-ajuste-aplicavel"));
    result.put("trafficClass", "AGENT_VALIDATION");
    result.put("internalMarker", "mh_internal_test");
    result.put("syntheticEvaluation", true);
    result.put("humanEvidenceClaimed", false);
    result.put("commercialEvidenceClaimed", false);
    result
        .putObject("experienceAssessment")
        .put("evidenceBoundary", "Modelo simulado apenas na sandbox.");
    var checks = result.putObject("checks");
    for (String check :
        List.of(
            "sameProductAndVersion",
            "isolatedFreshSession",
            "functionalOutcomeMatchesScenario",
            "lowEffortNoPrompting",
            "accessibilityAndResponsive",
            "privacyPreserved",
            "internalTrafficSegregated",
            "safeLimits",
            "noExternalSideEffects")) checks.put(check, true);
    result.set(
        "sideEffects",
        json.valueToTree(
            Map.of(
                "paymentEnabled",
                false,
                "published",
                false,
                "campaignCreated",
                false,
                "mediaSpendBrl",
                0)));
    var visual = result.putObject("visualAudit");
    visual.putArray("evidenceIds").add(910901L);
    for (String field : List.of("visualHierarchy", "legibility", "affectiveResponse", "trustCues"))
      visual.put(field, "Verificação sintética dos pixels gerados pelo navegador local.");
    result.putArray("evidence").add("Captura local 910901");
    result.putArray("requiredChanges");
    result.put(
        "rootCause", "Percurso local confirmado pelo harness; interpretação do modelo simulada.");
    Path fixture = directory.resolve("result.json");
    Files.writeString(fixture, json.writeValueAsString(result));
    Path executable = directory.resolve("fake-model.sh");
    Files.writeString(
        executable,
        "#!/bin/sh\ncat > '"
            + directory.resolve("prompt.txt")
            + "'\n"
            + "while [ $# -gt 0 ]; do if [ \"$1\" = --output-last-message ]; then shift; cp '"
            + fixture
            + "' \"$1\"; exit 0; fi; shift; done\nexit 1\n");
    assertThat(executable.toFile().setExecutable(true)).isTrue();
    return executable;
  }
}
