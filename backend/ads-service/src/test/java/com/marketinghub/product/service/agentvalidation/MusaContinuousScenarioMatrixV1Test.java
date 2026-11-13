package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Comprova rejeição de provas incompletas e consome a matriz local real quando disponível. */
class MusaContinuousScenarioMatrixV1Test {
  private final ObjectMapper json = new ObjectMapper();

  /** Contrato antigo, catálogo vazio e declaração isolada não comprovam o aplicativo contínuo. */
  @Test
  void rejectsEmptyOrHistoricalProof() throws Exception {
    assertThat(MusaContinuousScenarioMatrixV1.valid(json.readTree("{}"))).isFalse();
    assertThat(
            MusaContinuousScenarioMatrixV1.valid(
                json.readTree("{\"runtimeContract\":\"MUSA_CONTINUOUS_STYLE_EXECUTION_V1\"}")))
        .isFalse();
    assertThat(
            MusaContinuousScenarioMatrixV1.supports(
                json.readTree("{\"runtimeContract\":\"PDE_CONTEXTUAL_STYLE_EXECUTION_V1\"}")))
        .isFalse();
  }

  /** Reutiliza nove execuções reais e recusa perda de isolamento, contexto, custo e salvamento. */
  @Test
  void validatesRealEvidenceAndRejectsCorruptedProof() throws Exception {
    String path = System.getenv("MUSA_CONTINUOUS_LOCAL_REPORT");
    ObjectNode proof =
        (ObjectNode)
            (path == null || path.isBlank()
                ? json.readTree(
                    getClass().getResourceAsStream("/musa-continuous/real-local-matrix-v1.json"))
                : json.readTree(Path.of(path).toFile()));
    assertThat(MusaContinuousScenarioMatrixV1.valid(proof)).isTrue();
    ObjectNode broken = proof.deepCopy();
    ((ObjectNode) broken.path("scenarios").get(1))
        .put("evidenceId", proof.path("scenarios").get(0).path("evidenceId").asText());
    assertThat(MusaContinuousScenarioMatrixV1.valid(broken)).isFalse();
    broken = proof.deepCopy();
    ((ObjectNode) broken.path("scenarios").get(0).path("card"))
        .put("parentChoiceId", "another-owner");
    assertThat(MusaContinuousScenarioMatrixV1.valid(broken)).isFalse();
    broken = proof.deepCopy();
    ((ObjectNode) broken.path("scenarios").get(3)).put("repeatedWithoutDuplicate", false);
    assertThat(MusaContinuousScenarioMatrixV1.valid(broken)).isFalse();
    broken = proof.deepCopy();
    ((ObjectNode) broken.path("scenarios").get(6)).put("crossOwnerDenied", false);
    assertThat(MusaContinuousScenarioMatrixV1.valid(broken)).isFalse();
    broken = proof.deepCopy();
    ((ObjectNode) broken.path("scenarios").get(0).path("choices")).removeAll();
    assertThat(MusaContinuousScenarioMatrixV1.valid(broken)).isFalse();
    broken = proof.deepCopy();
    ((ObjectNode) broken.path("scenarios").get(0)).put("providerCalls", 1);
    assertThat(MusaContinuousScenarioMatrixV1.valid(broken)).isFalse();
  }
}
