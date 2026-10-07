package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Responsabilidade: validar o transporte e o callback comercial sem chamadas pagas. */
class CodexTransportIntegrationTest {
  @TempDir Path directory;
  private final ObjectMapper json = new ObjectMapper();

  /** Reproduz o erro do transporte antes de gerar resposta ou informar tokens. */
  @Test
  void rejectsUnsupportedFlexBeforeInference() throws Exception {
    Process process = new ProcessBuilder(model().toString(), "-c", "service_tier=\"flex\"").start();
    assertThat(process.waitFor()).isEqualTo(1);
    assertThat(new String(process.getInputStream().readAllBytes()))
        .contains("Unsupported service_tier: flex", "400");
    assertThat(directory.resolve("attempts.txt")).doesNotExist();
  }

  /** Percorre Mira, outra identidade e a revisão legada com auditoria e custo consistentes. */
  @ParameterizedTest
  @CsvSource({"10,99,6,false", "8017,9017,7017,false", "9,88,4,true"})
  void reportsIndependentReviewThroughCompatibleTransport(
      long product, long experiment, long cycle, boolean legacy) throws Exception {
    String processCode = legacy ? "landing-page-generation" : "pde-construction-approval";
    String activity = legacy ? "commercial" : "commercialIntegrityReview";
    Map<String, Object> task =
        Map.of(
            "taskId",
            990002L,
            "processCode",
            processCode,
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
                product == 10 ? "pde-planejado-36" : "produto-sintetico",
                "experienceVersion",
                "mira-commercial-v1",
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
    JsonNode expected = expected(task, legacy);
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
                exchange
                        .getRequestURI()
                        .getQuery()
                        .contains("processCode=" + processCode + "&activityId=" + activity)
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
    try {
      var properties = new MetaAdApproverProperties();
      properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
      new CommercialBpmTaskConsumer(
              properties,
              model().toString(),
              "unchanged-test-model",
              directory.toString(),
              "/absent",
              json)
          .processOne();
      assertThat(failure.get()).isNull();
      assertThat(callback.get()).isNotNull();
      assertThat(json.readTree(callback.get().path("resultJson").asText()))
          .isEqualTo(json.readTree(expected.toString()));
      var evidence = json.readTree(callback.get().path("evidenceJson").asText());
      assertThat(evidence.path("preferredServiceTier").asText()).isEqualTo("FLEX");
      assertThat(evidence.path("requestedServiceTier").asText()).isEqualTo("DEFAULT");
      assertThat(evidence.path("effectiveServiceTier").asText()).isEqualTo("STANDARD");
      assertThat(evidence.path("serviceTierException").asText()).contains("recusa Flex");
      assertThat(callback.get().path("modelUsages").get(0).path("serviceTier").asText())
          .isEqualTo("STANDARD");
      assertThat(callback.get().path("modelUsages").get(0).path("inputTokens").asInt())
          .isEqualTo(100);
      assertThat(audit.get().path("promptSent").asText()).contains("experiment:" + experiment);
      assertThat(Files.readAllLines(directory.resolve("attempts.txt"))).hasSize(1);
      assertThat(Files.readString(directory.resolve("arguments.json")))
          .contains("unchanged-test-model", "read-only")
          .doesNotContain("priority", "flex");
    } finally {
      server.stop(0);
    }
  }

  /** Monta apenas uma fixture de parecer independente, sem representar evidência de mercado. */
  private JsonNode expected(Map<String, Object> task, boolean legacy) throws Exception {
    var result =
        json.createObjectNode()
            .put("decision", "APPROVED")
            .put("commercialRationale", "Parecer simulado localmente, sem publicação.");
    result.putArray("evidence").add("Fixture técnica de transporte");
    result.putArray("requiredChanges");
    if (legacy) return result;
    result
        .withArray("evidence")
        .add("Fixture técnica")
        .add("Fixture do cenário aderente")
        .add("Fixtures de recuperação e segurança");
    var target = json.valueToTree(task.get("taskTarget"));
    result
        .put("contractVersion", "PDE_TEMIS_AGENT_VALIDATION_V1")
        .put("rootCause", "Interpretação simulada na sandbox.")
        .put("sourceReference", String.valueOf(task.get("sourceReference")))
        .put("productId", target.path("productId").asLong())
        .put("productSlug", target.path("productSlug").asText())
        .put("prototypeVersion", target.path("experienceVersion").asText())
        .put("trafficClass", "AGENT_VALIDATION")
        .put("internalMarker", "mh_internal_test")
        .put("humanEvidenceClaimed", false)
        .put("commercialEvidenceClaimed", false);
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
                .getResourceAsStream("/prompts/bpm/pde-agent-validation-review-v4-schema.json"));
    var checks = result.putObject("agentValidationChecks");
    schema
        .path("properties")
        .path("agentValidationChecks")
        .path("required")
        .forEach(key -> checks.put(key.asText(), true));
    return result;
  }

  /** Executa um transporte local que aceita default e recusa a configuração inválida. */
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
