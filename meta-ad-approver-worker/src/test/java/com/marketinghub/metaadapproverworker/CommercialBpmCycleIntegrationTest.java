package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: percorrer a fila e o callback real de Têmis com provas locais de Psique. */
@EnabledIfEnvironmentVariable(named = "VEGA_GATE_FLOW_ARTIFACTS", matches = ".+")
class CommercialBpmCycleIntegrationTest {
  @TempDir Path directory;
  private final ObjectMapper json = new ObjectMapper();

  /**
   * Reutiliza técnica e cenários reais; comprova o suplemento no request e simula modelo e fila.
   */
  @Test
  void reviewsActualCycleEvidenceAndReportsSyntheticContract() throws Exception {
    Path flow = Path.of(System.getenv("VEGA_GATE_FLOW_ARTIFACTS"));
    var technical = json.readTree(Files.readString(flow.resolve("TECHNICAL.json")));
    List<Map<String, Object>> evidence = new ArrayList<>();
    evidence.add(
        Map.of("taskId", 910388L, "activityId", "technicalHomologation", "result", technical));
    var activities =
        Map.of(
            "ADHERENT", "psiqueAdherent", "RECOVERY", "psiqueRecovery", "SAFETY", "psiqueSafety");
    for (String scenario : List.of("ADHERENT", "RECOVERY", "SAFETY")) {
      var result = json.readTree(Files.readString(flow.resolve(scenario + ".json")));
      assertThat(result.path("decision").asText()).isEqualTo("APPROVED");
      assertThat(result.path("prototypeVersion")).isEqualTo(technical.path("prototypeVersion"));
      evidence.add(
          Map.of(
              "taskId",
              910389L + evidence.size(),
              "activityId",
              activities.get(scenario),
              "result",
              result));
    }
    String reference = technical.path("sourceReference").asText();
    long productId = technical.path("productId").asLong();
    long experimentId = Long.parseLong(reference.substring("experiment:".length()));
    Map<String, Object> pdeContext = new LinkedHashMap<>();
    pdeContext.put(
        "lineage",
        Map.of("learningCycleId", 91002L, "productId", productId, "experimentId", experimentId));
    String controlsFile = System.getenv("PDE_OPERATIONAL_REPORT_FILE");
    boolean hasControls = controlsFile != null && !controlsFile.isBlank();
    if (hasControls) {
      var controls = json.readTree(Files.readString(Path.of(controlsFile)));
      assertThat(controls.path("productSlug")).isEqualTo(technical.path("productSlug"));
      assertThat(controls.path("prototypeVersion")).isEqualTo(technical.path("prototypeVersion"));
      assertThat(controls.path("criteria").size()).isEqualTo(7);
      pdeContext.put("operationalControlEvidence", controls);
    }
    String revision = hasControls ? "v5" : "v4";
    var task =
        Map.of(
            "taskId",
            910392L,
            "sourceReference",
            reference,
            "processCode",
            "pde-construction-approval",
            "processVersion",
            8,
            "activityId",
            "commercialIntegrityReview",
            "taskTarget",
            Map.of(
                "productId",
                productId,
                "experimentId",
                experimentId,
                "productSlug",
                technical.path("productSlug").asText(),
                "experienceVersion",
                technical.path("prototypeVersion").asText(),
                "pdeContext",
                pdeContext),
            "processContext",
            Map.of(
                "completedActivities",
                evidence,
                "validationPolicy",
                Map.of("mode", "AGENT_VALIDATION", "humanReadingsRequired", false)));
    var expected = json.createObjectNode();
    expected.put("contractVersion", "PDE_TEMIS_AGENT_VALIDATION_V1");
    expected.put("decision", "APPROVED");
    expected.put(
        "commercialRationale",
        "Parecer simulado sobre provas locais reais, sem alegação humana ou venda.");
    expected.put(
        "rootCause", "O contrato do ciclo preserva identidade, segregação e os checks do gate.");
    for (String key :
        List.of(
            "sourceReference",
            "productId",
            "productSlug",
            "prototypeVersion",
            "trafficClass",
            "internalMarker",
            "humanEvidenceClaimed",
            "commercialEvidenceClaimed",
            "sideEffects")) expected.set(key, technical.path(key));
    var schema =
        json.readTree(
            getClass()
                .getResourceAsStream("/prompts/bpm/pde-agent-validation-review-v4-schema.json"));
    var checks = expected.putObject("agentValidationChecks");
    schema
        .path("properties")
        .path("agentValidationChecks")
        .path("required")
        .forEach(key -> checks.put(key.asText(), true));
    expected
        .putArray("evidence")
        .add("Técnica local")
        .add("Aderente local")
        .add("Recuperação local")
        .add("Segurança local");
    expected.putArray("requiredChanges");
    Files.writeString(directory.resolve("expected.json"), expected.toString());
    Files.writeString(directory.resolve("expected-revision.txt"), revision);
    Path model = directory.resolve("model.py");
    Files.writeString(
        model,
        """
        #!/usr/bin/env python3
        import sys,json,pathlib,re
        root=pathlib.Path(__file__).parent
        prompt=sys.stdin.read()
        (root/'prompt.txt').write_text(prompt)
        schema=json.loads(pathlib.Path(sys.argv[sys.argv.index('--output-schema')+1]).read_text())
        expected=json.loads((root/'expected.json').read_text())
        revision=(root/'expected-revision.txt').read_text()
        assert re.fullmatch(schema['properties']['sourceReference']['pattern'],expected['sourceReference'])
        assert 'versionedArtifactEvidence' not in prompt
        assert 'integridade da validação multiagente PDE '+revision in prompt
        if revision=='v5':
            assert 'LOCAL_MYSQL57_WITH_CONTEXT_TEST_DOUBLES' in prompt
            assert 'CONCURRENT_SINGLE_CONSUMPTION' in prompt
            assert 'testReceiptSha256' in prompt
        pathlib.Path(sys.argv[sys.argv.index('--output-last-message')+1]).write_text((root/'expected.json').read_text())
        print('{"type":"turn.completed","usage":{"input_tokens":100,"output_tokens":100}}')
        """);
    assertThat(model.toFile().setExecutable(true)).isTrue();
    var callback = new AtomicReference<JsonNode>();
    var executionAudit = new AtomicReference<JsonNode>();
    var failure = new AtomicReference<String>();
    var versionedClaim = new AtomicBoolean();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/internal/agent-tasks/",
        exchange -> {
          byte[] body = exchange.getRequestBody().readAllBytes();
          String path = exchange.getRequestURI().getPath();
          Object response = Map.of();
          if (path.endsWith("/pending")) {
            versionedClaim.set(
                exchange.getRequestURI().getQuery().contains("workerContract=TEMIS_BPM_LEASE_V1"));
            response =
                exchange
                        .getRequestURI()
                        .getQuery()
                        .contains("processCode=pde-construction-approval")
                    ? List.of(task)
                    : List.of();
          } else if (path.endsWith("/execution-audit")) {
            executionAudit.set(json.readTree(body));
          } else if (path.endsWith("/result")) callback.set(json.readTree(body));
          else if (path.endsWith("/failure"))
            failure.set(new String(body, java.nio.charset.StandardCharsets.UTF_8));
          byte[] data = json.writeValueAsBytes(response);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, data.length);
          exchange.getResponseBody().write(data);
          exchange.close();
        });
    server.start();
    try {
      var properties = new MetaAdApproverProperties();
      properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
      new CommercialBpmTaskConsumer(
              properties,
              model.toString(),
              "test-double",
              directory.toString(),
              "/absent-global-catalog",
              json)
          .processOne();
      assertThat(failure.get()).isNull();
      assertThat(versionedClaim).isTrue();
      assertThat(executionAudit.get()).isNotNull();
      assertThat(executionAudit.get().path("promptSent").asText())
          .contains("integridade da validação multiagente PDE " + revision, reference);
      assertThat(callback.get()).isNotNull();
      assertThat(callback.get().path("executionAudit").toString())
          .contains("PDE " + revision, reference);
      var result = json.readTree(callback.get().path("resultJson").asText());
      assertThat(result).isEqualTo(expected);
      Files.writeString(flow.resolve("TEMIS.json"), result.toPrettyString());
    } finally {
      server.stop(0);
    }
  }
}
