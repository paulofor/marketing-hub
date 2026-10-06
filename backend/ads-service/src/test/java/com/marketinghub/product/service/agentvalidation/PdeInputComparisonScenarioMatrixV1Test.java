package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Comprova a comparação reutilizável sem depender de nome, identificador ou versão de produto. */
class PdeInputComparisonScenarioMatrixV1Test {
  /**
   * Confirma o contrato do relatório capturado pelo runner local, sem fabricar ou repetir provas.
   */
  @Test
  void acceptsExistingBrowserEvidenceWhenProvided() throws Exception {
    String path = System.getenv("MIRA_LOCAL_REPORT");
    org.junit.jupiter.api.Assumptions.assumeTrue(path != null && !path.isBlank());
    assertThat(
            PdeInputComparisonScenarioMatrixV1.valid(
                new ObjectMapper().readTree(java.nio.file.Path.of(path).toFile())))
        .isTrue();
  }

  /** Aceita inventário igual em dezoito sessões isoladas e preserva o contrato do produto. */
  @Test
  void acceptsCompleteIsolatedMatrix() {
    assertThat(PdeInputComparisonScenarioMatrixV1.valid(matrix("fixture-version-17"))).isTrue();
    assertThat(PdeInputComparisonScenarioMatrixV1.valid(matrix("another-product-version-2")))
        .isTrue();
  }

  /** Rejeita caso duplicado, sessão reutilizada e comparação feita com produtos diferentes. */
  @Test
  void rejectsDuplicateCasesAndUnequalInventories() {
    ObjectNode duplicate = matrix("fixture-version-17");
    scenario(duplicate, 1).put("condition", "REFERENCE");
    assertThat(PdeInputComparisonScenarioMatrixV1.valid(duplicate)).isFalse();
    ObjectNode reusedSession = matrix("fixture-version-17");
    scenario(reusedSession, 1)
        .put("evidenceId", scenario(reusedSession, 0).path("evidenceId").asText());
    assertThat(PdeInputComparisonScenarioMatrixV1.valid(reusedSession)).isFalse();
    ObjectNode otherInventory = matrix("fixture-version-17");
    ((ObjectNode) scenario(otherInventory, 1).path("products").get(0)).put("name", "Outro produto");
    assertThat(PdeInputComparisonScenarioMatrixV1.valid(otherInventory)).isFalse();
  }

  /**
   * Impede aprovação por hash igual quando a saída funcional ou as fronteiras forem divergentes.
   */
  @Test
  void rejectsDifferentOutputsPaidProvidersAndForgedEvidence() {
    for (String field :
        List.of(
            "providerCalls",
            "humanEvidenceClaimed",
            "commercialEvidenceClaimed",
            "organizationsUsed")) {
      ObjectNode forged = matrix("fixture-version-17");
      if (field.endsWith("Claimed")) scenario(forged, 0).put(field, true);
      else scenario(forged, 0).put(field, 2);
      assertThat(PdeInputComparisonScenarioMatrixV1.valid(forged)).as(field).isFalse();
    }
    ObjectNode changed = matrix("fixture-version-17");
    ((ObjectNode) scenario(changed, 1).path("routine").get(0))
        .put("documentedDirection", "Texto divergente");
    assertThat(PdeInputComparisonScenarioMatrixV1.valid(changed)).isFalse();
    ObjectNode late = matrix("fixture-version-17");
    scenario(late, 0).put("resultReadySeconds", 601);
    assertThat(PdeInputComparisonScenarioMatrixV1.valid(late)).isFalse();
    ObjectNode unsafe = matrix("fixture-version-17");
    scenario(unsafe, 17).put("safetyBlocked", false);
    assertThat(PdeInputComparisonScenarioMatrixV1.valid(unsafe)).isFalse();
  }

  /** Retorna um cenário editável da matriz sintética usada exclusivamente em testes. */
  private static ObjectNode scenario(ObjectNode root, int index) {
    return (ObjectNode) root.path("scenarios").get(index);
  }

  /** Constrói a matriz declarada com dados documentais sintéticos e efeitos externos nulos. */
  static ObjectNode matrix(String version) {
    ObjectMapper json = new ObjectMapper();
    ObjectNode root = json.createObjectNode();
    root.put("fixtureContract", PdeInputComparisonScenarioMatrixV1.CONTRACT);
    root.put("generationMode", "DETERMINISTIC_DOCUMENTED_LABELS");
    root.put("providerCalls", 0);
    root.put("prototypeVersion", version);
    var scenarios = root.putArray("scenarios");
    for (String code : List.of("ADHERENT", "RECOVERY", "SAFETY")) {
      for (String device : List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7")) {
        for (String condition : List.of("REFERENCE", "REDUCED")) {
          ObjectNode value = scenarios.addObject();
          boolean safety = "SAFETY".equals(code);
          value.put("scenarioCode", code);
          value.put("deviceProfile", device);
          value.put("condition", condition);
          value.put("prototypeVersion", version);
          value.put("evidenceId", code + device + condition);
          value.put("status", "PASS");
          value.put("trafficClass", "AGENT_VALIDATION");
          value.put("mhInternalTest", true);
          value.put("providerCalls", 0);
          value.put("humanEvidenceClaimed", false);
          value.put("commercialEvidenceClaimed", false);
          value.put("resultReadySeconds", 1);
          value.put("organizationsUsed", safety ? 0 : 1);
          value.put("safetyBlocked", safety);
          value.put("recovered", "RECOVERY".equals(code));
          value.put("manualFields", "REFERENCE".equals(condition) ? 4 : 2);
          value
              .putArray("products")
              .addObject()
              .put("name", "Produto documental")
              .put("labelDirections", "Enxágue.");
          var routine = value.putArray("routine");
          if (!safety)
            routine
                .addObject()
                .put("productName", "Produto documental")
                .put("documentedDirection", "Enxágue.");
          var effects = value.putObject("sideEffects");
          effects.put("paymentEnabled", false);
          effects.put("published", false);
          effects.put("campaignCreated", false);
          effects.put("mediaSpendBrl", 0);
        }
      }
    }
    return root;
  }
}
