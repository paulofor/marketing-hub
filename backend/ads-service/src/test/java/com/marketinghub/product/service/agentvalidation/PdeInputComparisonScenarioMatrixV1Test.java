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

  /** Aprova medições novas com identidades distintas e conserva a matriz legada antes válida. */
  @Test
  void acceptsMeasuredMatrixAndHistoricalMatrix() {
    assertThat(
            PdeInputComparisonScenarioMatrixV1.valid(measuredMatrix("mira-private-candidate-v2")))
        .isTrue();
    assertThat(
            PdeInputComparisonScenarioMatrixV1.valid(measuredMatrix("another-product-version-4")))
        .isTrue();
    assertThat(PdeInputComparisonScenarioMatrixV1.valid(matrix("mira-commercial-v1"))).isTrue();
    assertThat(PdeInputComparisonScenarioMatrixV1.valid(matrix("mira-private-candidate-v2")))
        .isFalse();
  }

  /** Exige sinais executados da candidata nova e rejeita checkout no caminho inseguro. */
  @Test
  void rejectsMissingSyntheticSignalsAndUnsafeContinuation() {
    for (String version : List.of("mira-private-candidate-v3", "another-product-version-9")) {
      var complete = signaledMatrix(version);
      assertThat(PdeInputComparisonScenarioMatrixV1.valid(complete)).isTrue();
      scenario(complete, 0).putArray("events").add("EXPERIENCE_STARTED").add("VALUE_MOMENT");
      assertThat(PdeInputComparisonScenarioMatrixV1.valid(complete)).isFalse();
      var unsafe = signaledMatrix(version);
      ((com.fasterxml.jackson.databind.node.ArrayNode) scenario(unsafe, 17).path("events"))
          .add("CHECKOUT_STARTED");
      assertThat(PdeInputComparisonScenarioMatrixV1.valid(unsafe)).isFalse();
    }
    assertThat(
            PdeInputComparisonScenarioMatrixV1.valid(measuredMatrix("mira-private-candidate-v3")))
        .isFalse();
    assertThat(
            PdeInputComparisonScenarioMatrixV1.valid(measuredMatrix("mira-private-candidate-v4")))
        .isFalse();
  }

  /** Adiciona sinais sintéticos para exercitar o contrato, sem fabricar prova de navegador. */
  private static ObjectNode signaledMatrix(String version) {
    var root = measuredMatrix(version);
    root.put("fixtureContract", PdeInputComparisonScenarioMatrixV1.SIGNAL_CONTRACT);
    for (var row : root.path("scenarios")) {
      var events = ((ObjectNode) row).putArray("events").add("EXPERIENCE_STARTED");
      if ("SAFETY".equals(row.path("scenarioCode").asText())) events.add("SAFETY_LIMIT_BLOCKED");
      else
        events
            .add("VALUE_MOMENT")
            .add("READY_RESULT_USED")
            .add("PREFERRED_OVER_FREE")
            .add("CHECKOUT_STARTED");
    }
    return root;
  }

  /** Bloqueia a antiga ambiguidade entre mínimo e preenchimento e a mistura de relógios. */
  @Test
  void rejectsAmbiguousMeasurementsAndMixedClocks() {
    for (String field :
        List.of(
            "filledProductFields",
            "providedProductCount",
            "minimumRequiredProductFields",
            "actualFilledFields",
            "requiredObjectiveFields",
            "scenarioCompletedSeconds")) {
      var forged = measuredMatrix("mira-private-candidate-v2");
      scenario(forged, 1).put(field, 0);
      assertThat(PdeInputComparisonScenarioMatrixV1.valid(forged)).as(field).isFalse();
    }
    var mixed = measuredMatrix("mira-private-candidate-v2");
    scenario(mixed, 0).put("measurementClock", "BACKEND_DATE_VS_WORKER_DATE");
    assertThat(PdeInputComparisonScenarioMatrixV1.valid(mixed)).isFalse();
    var ambiguous = measuredMatrix("mira-private-candidate-v2");
    scenario(ambiguous, 0).put("manualFields", 2);
    assertThat(PdeInputComparisonScenarioMatrixV1.valid(ambiguous)).isFalse();
  }

  /** Adiciona medições sintéticas explícitas sem tratá-las como evidência de navegador real. */
  private static ObjectNode measuredMatrix(String version) {
    var root = matrix(version);
    root.put("fixtureContract", PdeInputComparisonScenarioMatrixV1.MEASURED_CONTRACT);
    for (var row : root.path("scenarios")) {
      var value = (ObjectNode) row;
      ((com.fasterxml.jackson.databind.node.ArrayNode) value.path("products"))
          .addObject()
          .put("name", "Segundo produto")
          .put("labelDirections", "Limpar e enxaguar.");
      if (!"SAFETY".equals(value.path("scenarioCode").asText()))
        ((com.fasterxml.jackson.databind.node.ArrayNode) value.path("routine"))
            .addObject()
            .put("productName", "Segundo produto")
            .put("documentedDirection", "Limpar e enxaguar.");
      value.remove("manualFields");
      value.put(
          "minimumRequiredProductFields",
          "REFERENCE".equals(value.path("condition").asText()) ? 4 : 2);
      value.put("requiredObjectiveFields", 1);
      value.put("objectivePrefilled", true);
      value.put("providedProductCount", 2);
      value.put("filledProductFields", 4);
      value.put("filledOptionalSourceFields", 0);
      value.put("editedObjectiveFields", 0);
      value.put("actualFilledFields", 4);
      value.put("corrections", 0);
      value.put("measurementClock", "MONOTONIC_WORKER");
      value.put("outcomeBoundary", "FIRST_VISIBLE_RESULT_OR_SAFE_BLOCK");
      value.put("scenarioCompletedSeconds", 2);
    }
    return root;
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
