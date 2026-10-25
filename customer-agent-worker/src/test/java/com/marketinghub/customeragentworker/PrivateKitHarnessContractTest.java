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

  /** Comprova a matriz real e recusa pacote, captura ou verificação da interface incompletos. */
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
    assertThat(artifacts).hasSize(15);
    for (String failure :
        List.of(
            "previous-format",
            "previous-count",
            "missing-capture",
            "missing-moment",
            "missing-surface",
            "failed-surface",
            "wrong-device")) {
      var invalid = report.deepCopy();
      if ("previous-format".equals(failure))
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalid).remove("packageContractVersion");
      else if ("previous-count".equals(failure))
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("scenarios").get(0))
            .put("packageFileCount", 24);
      else if ("missing-moment".equals(failure))
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("artifacts").get(0))
            .put("evidenceKey", "unknown");
      else if ("wrong-device".equals(failure))
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("artifacts").get(0))
            .put("deviceProfile", "UNKNOWN");
      else if ("missing-surface".equals(failure))
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("scenarios").get(0))
            .remove("customerSurface");
      else if ("failed-surface".equals(failure))
        ((com.fasterxml.jackson.databind.node.ObjectNode)
                invalid.path("scenarios").get(0).path("customerSurface"))
            .put("internalInformationAbsent", false);
      else
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("scenarios").get(0))
            .remove("screenshotEvidenceKeys");
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

  /** Reproduz os relatórios reais dos dois perfis em contexto complementar, sem navegador ou IA. */
  @Test
  void runsSupplementalCaptureWithExistingLocalReports() throws Exception {
    String local = System.getenv("KIT_LOCAL_REPORT");
    org.junit.jupiter.api.Assumptions.assumeTrue(local != null);
    for (String name : List.of("report.json", "other-product-report.json")) {
      Path fixture = Path.of(local).resolveSibling(name);
      var report = json.readTree(fixture.toFile());
      long product = report.path("productId").asLong();
      long experiment = Long.parseLong(report.path("sourceReference").asText().split(":")[1]);
      String reference = "experiment:" + experiment + "@video-proof-v1";
      Path script = work.resolve("private-kit-harness.mjs");
      Files.writeString(
          script,
          """
          import sys,json,pathlib,shutil
          data=json.load(open(sys.argv[1])); result=json.load(open(%s)); out=pathlib.Path(sys.argv[3]); out.mkdir(parents=True,exist_ok=True)
          result['sourceReference']=data['sourceReference']
          for artifact in result['artifacts']:
              target=out/(artifact['evidenceKey']+'.png'); shutil.copy(artifact['localPath'],target)
              artifact['localPath']=str(target); artifact['captureSessionId']=data['captureSessionId']
          json.dump(result,open(sys.argv[2],'w'))
          """
              .formatted(json.writeValueAsString(fixture.toAbsolutePath().toString())));
      var target =
          Map.of(
              "productId",
              product,
              "productSlug",
              report.path("productSlug").asText(),
              "experienceVersion",
              report.path("prototypeVersion").asText(),
              "publicUrl",
              report.path("publicUrl").asText(),
              "experimentId",
              experiment,
              "pdeContext",
              Map.of(
                  "lineage",
                  Map.of(
                      "productId",
                      product,
                      "learningCycleId",
                      report.path("cycleId").asLong(),
                      "experimentId",
                      experiment),
                  "privatePrototypeAcceptance",
                  Map.of(
                      "runtimeKind",
                      "DETERMINISTIC_PRIVATE_KIT_V1",
                      "profileCode",
                      report.path("profileCode").asText(),
                      "prototypeVersion",
                      report.path("prototypeVersion").asText())));
      var task = Map.<String, Object>of("sourceReference", reference, "taskTarget", target);
      var runner =
          new PdeAgentValidationHarnessRunner(
              json, "/usr/bin/python3", script.toString(), "fixture", true);
      var result = runner.run(task, "TECHNICAL", null, work.resolve(name));
      assertThat(result.result().path("sourceReference").asText()).isEqualTo(reference);
      assertThat(result.visualEvidence().capture().artifacts()).hasSize(15);
      assertThatThrownBy(() -> runner.run(task, "SCENARIO", "ADHERENT", work.resolve("invalid")))
          .hasMessageContaining("canônica");
      var wrong = new HashMap<>(task);
      wrong.put("sourceReference", "experiment:1@video-proof-v1");
      assertThatThrownBy(() -> runner.run(wrong, "TECHNICAL", null, work.resolve("wrong")))
          .hasMessageContaining("kit privado");
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
