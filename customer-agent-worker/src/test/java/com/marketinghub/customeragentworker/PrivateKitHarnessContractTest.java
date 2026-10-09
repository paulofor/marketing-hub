package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Responsabilidade: validar o relatório executável e recusar despacho com identidade incompatível.
 */
class PrivateKitHarnessContractTest {
  @TempDir Path work;
  private final ObjectMapper json = new ObjectMapper();

  /** Recusa prova antiga, captura ausente e segurança sem explicação ou continuidade preservada. */
  @Test
  void validatesLocalNineCaseReport() throws Exception {
    String path = System.getenv("KIT_LOCAL_REPORT");
    org.junit.jupiter.api.Assumptions.assumeTrue(
        path != null, "Matriz real executada pelo runner local.");
    JsonNode report = json.readTree(Files.readString(Path.of(path)));
    Path screenshot = Path.of(report.path("artifacts").get(0).path("localPath").asText());
    var runner = new PdeAgentValidationHarnessRunner(json, "node", "absent", "synthetic", true);
    Map<String, Object> expected = new HashMap<>();
    for (String field :
        List.of("sourceReference", "productSlug", "prototypeVersion", "profileCode"))
      expected.put(field, report.path(field).asText());
    expected.put("productId", report.path("productId").asLong());
    expected.put("cycleId", report.path("cycleId").asLong());
    expected.put("sourceUrl", report.path("publicUrl").asText());
    expected.put("runtimeKind", "DETERMINISTIC_PRIVATE_KIT_V1");
    List<?> artifacts =
        ReflectionTestUtils.invokeMethod(
            runner,
            "validateOutput",
            report,
            report.path("artifacts").get(0).path("captureSessionId").asText(),
            screenshot.getParent(),
            "TECHNICAL",
            null,
            expected);
    assertThat(artifacts).hasSize(9);
    for (String failure :
        List.of(
            "previous-format",
            "previous-count",
            "missing-capture",
            "missing-safety",
            "wrong-cycle-action",
            "lost-block")) {
      var invalid = report.deepCopy();
      if ("previous-format".equals(failure))
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalid).remove("packageContractVersion");
      else if ("previous-count".equals(failure))
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("scenarios").get(0))
            .put("packageFileCount", 24);
      else if ("missing-capture".equals(failure))
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("scenarios").get(0))
            .remove("screenshotEvidenceKeys");
      else {
        var safety =
            java.util.stream.StreamSupport.stream(invalid.path("scenarios").spliterator(), false)
                .filter(s -> "SAFETY".equals(s.path("scenarioCode").asText()))
                .findFirst()
                .orElseThrow();
        if ("missing-safety".equals(failure))
          ((com.fasterxml.jackson.databind.node.ObjectNode) safety).remove("safetyOutcome");
        else if ("wrong-cycle-action".equals(failure))
          ((com.fasterxml.jackson.databind.node.ObjectNode) safety.path("safetyOutcome"))
              .put(
                  "nextActionPath",
                  "/business-process-chains/learning-cycles?productId=999&cycleId=888");
        else
          ((com.fasterxml.jackson.databind.node.ObjectNode) safety.path("safetyOutcome"))
              .put("persistedAfterReload", false);
      }
      assertThatThrownBy(
              () ->
                  ReflectionTestUtils.invokeMethod(
                      runner,
                      "validateOutput",
                      invalid,
                      report.path("artifacts").get(0).path("captureSessionId").asText(),
                      screenshot.getParent(),
                      "TECHNICAL",
                      null,
                      expected))
          .isInstanceOf(PdeAgentValidationHarnessRunner.HarnessException.class);
    }
  }

  /** Não permite que o runtime de kit contorne o vínculo entre produto, ciclo e experimento. */
  @Test
  void refusesMismatchedLineageBeforeBrowser() {
    var target =
        Map.of(
            "productId",
            71L,
            "productSlug",
            "different-kit",
            "experienceVersion",
            "v1",
            "publicUrl",
            "http://127.0.0.1:57282/api/pde/kit/private/v1/prototype",
            "experimentId",
            98L,
            "pdeContext",
            Map.of(
                "lineage",
                Map.of("productId", 72L, "learningCycleId", 55L, "experimentId", 98L),
                "privatePrototypeAcceptance",
                Map.of(
                    "runtimeKind",
                    "DETERMINISTIC_PRIVATE_KIT_V1",
                    "profileCode",
                    "nails-v1",
                    "prototypeVersion",
                    "v1")));
    var runner =
        new PdeAgentValidationHarnessRunner(json, "/must-not-run", "/absent", "synthetic", true);
    assertThatThrownBy(
            () ->
                runner.run(
                    Map.of("sourceReference", "experiment:98", "taskTarget", target),
                    "TECHNICAL",
                    null,
                    work))
        .hasMessageContaining("não corresponde");
  }
}
