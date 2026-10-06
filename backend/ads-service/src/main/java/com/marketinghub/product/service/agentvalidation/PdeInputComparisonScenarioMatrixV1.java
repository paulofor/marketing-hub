package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Valida a comparação documental isolada entre duas condições de entrada do mesmo protótipo. */
final class PdeInputComparisonScenarioMatrixV1 {
  static final String CONTRACT = "PDE_DOCUMENTED_INPUT_COMPARISON_V1";
  private static final List<String> SCENARIOS = List.of("ADHERENT", "RECOVERY", "SAFETY");
  private static final List<String> DEVICES = List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7");
  private static final List<String> CONDITIONS = List.of("REFERENCE", "REDUCED");

  /** Impede instanciação do validador sem estado. */
  private PdeInputComparisonScenarioMatrixV1() {}

  /**
   * Exige as dezoito combinações e compara entradas e saídas reais, sem confiar apenas em flags.
   */
  static boolean valid(JsonNode result) {
    JsonNode scenarios = result.path("scenarios");
    if (!CONTRACT.equals(result.path("fixtureContract").asText())
        || !"DETERMINISTIC_DOCUMENTED_LABELS".equals(result.path("generationMode").asText())
        || result.path("providerCalls").asInt(-1) != 0
        || !scenarios.isArray()
        || scenarios.size() != 18) {
      return false;
    }
    Map<String, JsonNode> cases = new HashMap<>();
    var evidenceIds = new HashSet<String>();
    for (JsonNode scenario : scenarios) {
      String code = scenario.path("scenarioCode").asText();
      String device = scenario.path("deviceProfile").asText();
      String condition = scenario.path("condition").asText();
      if (!SCENARIOS.contains(code)
          || !DEVICES.contains(device)
          || !CONDITIONS.contains(condition)
          || !validScenario(scenario, result.path("prototypeVersion").asText())
          || !evidenceIds.add(scenario.path("evidenceId").asText())
          || cases.putIfAbsent(code + "|" + device + "|" + condition, scenario) != null) {
        return false;
      }
    }
    for (String code : SCENARIOS) {
      for (String device : DEVICES) {
        JsonNode reference = cases.get(code + "|" + device + "|REFERENCE");
        JsonNode reduced = cases.get(code + "|" + device + "|REDUCED");
        if (reference == null
            || reduced == null
            || reduced.path("manualFields").asInt(-1) < 1
            || reference.path("manualFields").asInt(-1) <= reduced.path("manualFields").asInt(-1)
            || !reference.path("products").equals(reduced.path("products"))
            || !reference.path("routine").equals(reduced.path("routine"))) {
          return false;
        }
      }
    }
    return true;
  }

  /** Confirma prazo, segregação, consumo útil e recuperação ou bloqueio de cada cenário. */
  private static boolean validScenario(JsonNode value, String version) {
    boolean safety = "SAFETY".equals(value.path("scenarioCode").asText());
    JsonNode effects = value.path("sideEffects");
    JsonNode seconds = value.path("resultReadySeconds");
    JsonNode products = value.path("products");
    JsonNode routine = value.path("routine");
    return !version.isBlank()
        && version.equals(value.path("prototypeVersion").asText())
        && !value.path("evidenceId").asText().isBlank()
        && "PASS".equals(value.path("status").asText())
        && "AGENT_VALIDATION".equals(value.path("trafficClass").asText())
        && value.path("mhInternalTest").asBoolean(false)
        && value.path("providerCalls").asInt(-1) == 0
        && !value.path("humanEvidenceClaimed").asBoolean(true)
        && !value.path("commercialEvidenceClaimed").asBoolean(true)
        && seconds.isNumber()
        && seconds.asDouble(-1) >= 0
        && seconds.asDouble(601) <= 600
        && products.isArray()
        && !products.isEmpty()
        && routine.isArray()
        && (safety ? routine.isEmpty() : routine.size() == products.size())
        && value.path("organizationsUsed").asInt(-1) == (safety ? 0 : 1)
        && (!safety || value.path("safetyBlocked").asBoolean(false))
        && (!"RECOVERY".equals(value.path("scenarioCode").asText())
            || value.path("recovered").asBoolean(false))
        && !effects.path("paymentEnabled").asBoolean(true)
        && !effects.path("published").asBoolean(true)
        && !effects.path("campaignCreated").asBoolean(true)
        && effects.path("mediaSpendBrl").isNumber()
        && effects.path("mediaSpendBrl").decimalValue().signum() == 0;
  }
}
