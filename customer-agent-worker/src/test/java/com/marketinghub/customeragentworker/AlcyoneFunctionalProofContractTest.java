package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: exigir que as provas mostrem entrada, resultado e encerramento do percurso. */
class AlcyoneFunctionalProofContractTest {
  private final ObjectMapper json = new ObjectMapper();
  private final PdeAgentValidationHarnessRunner runner =
      new PdeAgentValidationHarnessRunner(json, "node", "/absent", "local-test", true);
  @TempDir Path temporary;

  /** Confere os três dispositivos e recusa momento ausente, duplicado ou de outro cenário. */
  @Test
  void validatesFunctionalMomentsAndPreservesReportsWithoutExtraCaptures() {
    var report = moments();
    assertThat(
            (Integer) ReflectionTestUtils.invokeMethod(runner, "validateKitCaptureMoments", report))
        .isEqualTo(6);
    assertThat(
            (Integer)
                ReflectionTestUtils.invokeMethod(
                    runner, "validateKitCaptureMoments", json.createObjectNode()))
        .isZero();
    for (String failure : List.of("missing", "duplicate", "foreign-device", "unlinked")) {
      var invalid = report.deepCopy();
      var artifacts = (com.fasterxml.jackson.databind.node.ArrayNode) invalid.path("artifacts");
      if (failure.equals("missing")) artifacts.remove(0);
      if (failure.equals("duplicate")) artifacts.add(artifacts.get(0).deepCopy());
      if (failure.equals("foreign-device"))
        ((ObjectNode) artifacts.get(0)).put("deviceProfile", "PIXEL_7");
      if (failure.equals("unlinked"))
        ((ObjectNode) invalid.path("scenarios").get(0)).putArray("screenshotEvidenceKeys");
      assertThatThrownBy(
              () -> ReflectionTestUtils.invokeMethod(runner, "validateKitCaptureMoments", invalid))
          .isInstanceOf(PdeAgentValidationHarnessRunner.HarnessException.class);
    }
  }

  /** Valida os PNGs e toda a matriz realmente executada localmente, sem chamadas externas. */
  @Test
  void validatesRealLocalReport() throws Exception {
    String path = System.getenv("ALCYONE_LOCAL_REPORT");
    org.junit.jupiter.api.Assumptions.assumeTrue(
        path != null, "Relatório da matriz local opcional.");
    var report = json.readTree(Path.of(path).toFile());
    var first = report.path("artifacts").get(0);
    Map<String, Object> expected = new HashMap<>();
    for (String field : List.of("sourceReference", "productSlug", "prototypeVersion"))
      expected.put(field, report.path(field).asText());
    expected.put("productId", report.path("productId").asLong());
    expected.put("sourceUrl", report.path("publicUrl").asText());
    List<?> output =
        ReflectionTestUtils.invokeMethod(
            runner,
            "validateOutput",
            report,
            first.path("captureSessionId").asText(),
            Path.of(first.path("localPath").asText()).getParent(),
            "TECHNICAL",
            null,
            expected);
    assertThat(output).hasSize(15);
  }

  /** Mantém a captura complementar restrita à identidade exata do experimento. */
  @Test
  void refusesSupplementalCaptureForDifferentExperimentBeforeBrowser() {
    var task =
        Map.<String, Object>of(
            "sourceReference",
            "experiment:91097@video-proof-v1",
            "taskTarget",
            Map.of(
                "productId",
                11L,
                "productSlug",
                "pde-planejado-46",
                "experienceVersion",
                "alcyone-private-v3",
                "publicUrl",
                "http://127.0.0.1:5184",
                "experimentId",
                91098L));
    assertThatThrownBy(() -> runner.run(task, "TECHNICAL", null, temporary))
        .hasMessageContaining("Captura complementar");
    assertThatThrownBy(() -> runner.run(task, "SCENARIO", "ADHERENT", temporary))
        .hasMessageContaining("canônica");
  }

  /** Monta apenas os metadados sintéticos que vinculam os momentos ao cenário aderente. */
  private ObjectNode moments() {
    var report = json.createObjectNode();
    report.put("captureMomentsContract", "PDE_KIT_CAPTURE_MOMENTS_V1");
    report.put("fixtureContract", "PDE_STATIC_RESULT_FIXTURES_V1");
    var artifacts = report.putArray("artifacts");
    var scenarios = report.putArray("scenarios");
    for (String device : List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7")) {
      var scenario =
          scenarios.addObject().put("scenarioCode", "ADHERENT").put("deviceProfile", device);
      var keys = scenario.putArray("screenshotEvidenceKeys");
      for (String moment : List.of("BRIEFING", "CONTENT", "FULL_PAGE")) {
        String key = "ADHERENT-" + device + "-" + moment;
        artifacts.addObject().put("evidenceKey", key).put("deviceProfile", device);
        keys.add(key);
      }
    }
    return report;
  }
}
