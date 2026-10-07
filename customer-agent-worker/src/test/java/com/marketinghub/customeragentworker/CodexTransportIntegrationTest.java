package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Responsabilidade: validar fila, processo Codex, auditoria e callback sem inferência paga. */
class CodexTransportIntegrationTest {
  @TempDir Path directory;
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  /** Reproduz a recusa observada no provedor antes de produzir resposta ou consumo. */
  @Test
  void reproducesUnsupportedFlexBeforeInference() throws Exception {
    Process process = new ProcessBuilder(model().toString(), "-c", "service_tier=\"flex\"").start();
    assertThat(process.waitFor()).isEqualTo(1);
    assertThat(new String(process.getInputStream().readAllBytes()))
        .contains("Unsupported service_tier: flex", "400");
    assertThat(directory.resolve("attempts.txt")).doesNotExist();
  }

  /** Percorre Mira, outra identidade e um contrato antes válido com o transporte restritivo. */
  @ParameterizedTest
  @CsvSource({
    "ADHERENT,10,99,6,pde-planejado-36,mira-commercial-v1",
    "RECOVERY,8017,9017,7017,produto-sintetico,mira-commercial-v1",
    "SAFETY,91004,91092,91002,metodo-musa-7-dias,musa-pde-entry-v11-primeiro-ajuste-aplicavel"
  })
  void deliversReviewedScenarioWithConsistentTierAudit(
      String scenario, long product, long experiment, long cycle, String slug, String version)
      throws Exception {
    String activity =
        "psique"
            + switch (scenario) {
              case "ADHERENT" -> "Adherent";
              case "RECOVERY" -> "Recovery";
              default -> "Safety";
            };
    Map<String, Object> task =
        Map.of(
            "taskId",
            990001L,
            "processCode",
            "pde-construction-approval",
            "processVersion",
            8,
            "activityId",
            activity,
            "sourceReference",
            "experiment:" + experiment,
            "taskTarget",
            Map.of(
                "productId",
                product,
                "experimentId",
                experiment,
                "productSlug",
                slug,
                "experienceVersion",
                version,
                "pdeContext",
                Map.of(
                    "lineage",
                    Map.of(
                        "productId",
                        product,
                        "experimentId",
                        experiment,
                        "learningCycleId",
                        cycle))));
    JsonNode expected = expected(task, scenario);
    Files.writeString(directory.resolve("expected.json"), expected.toString());
    var callback = new AtomicReference<JsonNode>();
    var failure = new AtomicReference<JsonNode>();
    var audit = new AtomicReference<JsonNode>();
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
          else if (path.endsWith("/failure")) failure.set(json.readTree(body));
          else if (path.endsWith("/execution-audit")) audit.set(json.readTree(body));
          byte[] bytes = json.writeValueAsBytes(response);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    var harness = mock(PdeAgentValidationHarnessRunner.class);
    var upload = mock(BpmVisualEvidenceBackendClient.class);
    Path image = directory.resolve("local-fixture.png");
    Files.write(
        image,
        java.util.Base64.getDecoder()
            .decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII="));
    var artifact =
        new BpmVisualEvidenceRunner.VisualArtifact(
            "local-capture",
            scenario,
            "FULL_PAGE",
            "DESKTOP_1440",
            1,
            null,
            1,
            1,
            1,
            0,
            "http://127.0.0.1/fixture",
            "http://127.0.0.1/fixture",
            Instant.now(),
            image.toString());
    var bundle =
        new BpmVisualEvidenceRunner.VisualEvidenceBundle(
            new BpmVisualEvidenceRunner.CaptureOutput(
                "local-capture", "DESKTOP_1440", List.of(), List.of(artifact)),
            directory.resolve("capture"));
    var raw = json.createObjectNode();
    raw.putArray("artifacts").add(json.valueToTree(artifact));
    raw.putArray("scenarios")
        .add(
            json.createObjectNode()
                .put("scenarioCode", scenario)
                .set("screenshotEvidenceKeys", json.valueToTree(List.of(scenario))));
    when(harness.run(anyMap(), eq("SCENARIO"), eq(scenario), any()))
        .thenReturn(
            new PdeAgentValidationHarnessRunner.HarnessExecution(raw, bundle, "{}", "local"));
    var receipt =
        new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
            990901L,
            "local-capture",
            scenario,
            "FULL_PAGE",
            "Fixture sintética",
            "DESKTOP_1440",
            1,
            null,
            1,
            1,
            1,
            0,
            artifact.sourceUrl(),
            artifact.finalUrl(),
            "/local-evidence",
            Files.size(image),
            "a".repeat(64),
            artifact.capturedAt(),
            image.toString());
    when(upload.uploadArtifacts(eq(990001L), anyList())).thenReturn(List.of(receipt));
    try {
      new CustomerBpmTaskConsumer(
              "http://127.0.0.1:" + server.getAddress().getPort(),
              model().toString(),
              "unchanged-test-model",
              "max",
              directory.toString(),
              "/absent",
              json,
              null,
              upload,
              harness,
              new CodexProcessSupervisor(
                  Duration.ofSeconds(5), Duration.ofSeconds(10), Duration.ofMillis(20)))
          .processOne();
      assertThat(failure.get()).isNull();
      assertThat(callback.get()).isNotNull();
      assertThat(json.readTree(callback.get().path("resultJson").asText()))
          .isEqualTo(json.readTree(expected.toString()));
      JsonNode evidence = json.readTree(callback.get().path("evidenceJson").asText());
      assertThat(evidence.path("preferredServiceTier").asText()).isEqualTo("FLEX");
      assertThat(evidence.path("requestedServiceTier").asText()).isEqualTo("DEFAULT");
      assertThat(evidence.path("effectiveServiceTier").asText()).isEqualTo("STANDARD");
      assertThat(evidence.path("serviceTierException").asText()).contains("recusa Flex");
      assertThat(callback.get().path("modelUsages").get(0).path("serviceTier").asText())
          .isEqualTo("STANDARD");
      assertThat(callback.get().path("modelUsages").get(0).path("inputTokens").asInt())
          .isEqualTo(100);
      assertThat(audit.get().path("promptSent").asText())
          .contains("screenshotEvidenceIds", "990901");
      assertThat(Files.readAllLines(directory.resolve("attempts.txt"))).hasSize(1);
      assertThat(Files.readString(directory.resolve("arguments.json")))
          .contains("unchanged-test-model", "read-only")
          .doesNotContain("priority", "flex");
    } finally {
      server.stop(0);
    }
  }

  /** Simula somente a resposta de interpretação, sem alegar leitura humana ou venda. */
  private JsonNode expected(Map<String, Object> task, String scenario) throws Exception {
    var target = json.valueToTree(task.get("taskTarget"));
    var result =
        json.createObjectNode()
            .put("contractVersion", "PDE_PSIQUE_AGENT_SCENARIO_V1")
            .put("decision", "APPROVED")
            .put("scenarioCode", scenario)
            .put("sourceReference", String.valueOf(task.get("sourceReference")))
            .put("productId", target.path("productId").asLong())
            .put("productSlug", target.path("productSlug").asText())
            .put("prototypeVersion", target.path("experienceVersion").asText())
            .put("trafficClass", "AGENT_VALIDATION")
            .put("internalMarker", "mh_internal_test")
            .put("syntheticEvaluation", true)
            .put("humanEvidenceClaimed", false)
            .put("commercialEvidenceClaimed", false)
            .put("rootCause", "Interpretação simulada localmente.");
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
    var schema =
        json.readTree(
            getClass()
                .getResourceAsStream(
                    "/prompts/bpm/v5/pde-agent-validation-scenario-review-schema.json"));
    var checks = result.putObject("checks");
    schema
        .path("properties")
        .path("checks")
        .path("required")
        .forEach(key -> checks.put(key.asText(), true));
    var assessment = result.putObject("experienceAssessment");
    schema
        .path("properties")
        .path("experienceAssessment")
        .path("required")
        .forEach(
            key -> {
              if ("array"
                  .equals(
                      schema
                          .path("properties")
                          .path("experienceAssessment")
                          .path("properties")
                          .path(key.asText())
                          .path("type")
                          .asText()))
                assessment.putArray(key.asText()).add("Limite da fixture sintética local.");
              else assessment.put(key.asText(), "Limite da fixture sintética local.");
            });
    var visual = result.putObject("visualAudit").put("captureSessionId", "local-capture");
    visual.putArray("evidenceIds").add(990901L);
    for (String key : List.of("visualHierarchy", "legibility", "affectiveResponse", "trustCues"))
      visual.put(key, "Fixture técnica local, sem validação de mercado.");
    result.putArray("evidence").add("Prova do transporte local");
    result.putArray("requiredChanges");
    return result;
  }

  /** Materializa o provedor restritivo que rejeita Flex e exige sandbox somente leitura. */
  private Path model() throws Exception {
    Path path = directory.resolve("model.py");
    Files.copy(
        Path.of("src/test/resources/codex/transport-double.py"),
        path,
        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    assertThat(path.toFile().setExecutable(true)).isTrue();
    return path;
  }
}
