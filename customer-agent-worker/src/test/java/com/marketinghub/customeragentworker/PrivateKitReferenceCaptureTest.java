package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: exigir arte ampliada vinculada aos cenários da biblioteca privada v2. */
class PrivateKitReferenceCaptureTest {
  /**
   * Recusa captura ausente na primeira aplicação ou na recuperação, sem aceitar prova de outro
   * dispositivo.
   */
  @Test
  void requiresLinkedReferenceArtworkAcrossDevices() {
    var json = new ObjectMapper();
    var report =
        json.createObjectNode()
            .put("captureMomentsContract", "PDE_KIT_CAPTURE_MOMENTS_V1")
            .put("profileCode", "beauty-v2");
    var artifacts = report.putArray("artifacts");
    var scenarios = report.putArray("scenarios");
    for (String device : List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7")) {
      for (String scenario : List.of("ADHERENT", "RECOVERY")) {
        var keys =
            scenarios
                .addObject()
                .put("scenarioCode", scenario)
                .put("deviceProfile", device)
                .putArray("screenshotEvidenceKeys");
        for (String suffix :
            scenario.equals("ADHERENT")
                ? List.of("-BRIEFING", "-CONTENT", "", "-ART")
                : List.of("", "-ART")) {
          String key = scenario + "-" + device + suffix;
          keys.add(key);
          artifacts.addObject().put("evidenceKey", key).put("deviceProfile", device);
        }
      }
    }
    var runner = new PdeAgentValidationHarnessRunner(json, "node", "/absent", "local-test", true);
    assertThat(
            (Integer) ReflectionTestUtils.invokeMethod(runner, "validateKitCaptureMoments", report))
        .isEqualTo(12);
    for (String scenario : List.of("ADHERENT", "RECOVERY")) {
      var broken = report.deepCopy();
      for (var item : broken.path("scenarios"))
        if (scenario.equals(item.path("scenarioCode").asText())) {
          var keys = (ArrayNode) item.path("screenshotEvidenceKeys");
          keys.remove(keys.size() - 1);
          break;
        }
      assertThatThrownBy(
              () -> ReflectionTestUtils.invokeMethod(runner, "validateKitCaptureMoments", broken))
          .isInstanceOf(PdeAgentValidationHarnessRunner.HarnessException.class);
    }
  }
}
