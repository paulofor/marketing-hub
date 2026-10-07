package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Valida a comparação documental isolada entre duas condições de entrada do mesmo protótipo. */
final class PdeInputComparisonScenarioMatrixV1 {
  static final String CONTRACT = "PDE_DOCUMENTED_INPUT_COMPARISON_V1";
  static final String MEASURED_CONTRACT = "PDE_DOCUMENTED_INPUT_COMPARISON_V2";
  static final String SIGNAL_CONTRACT = "PDE_DOCUMENTED_INPUT_COMPARISON_V3";
  private static final List<String> SCENARIOS = List.of("ADHERENT", "RECOVERY", "SAFETY");
  private static final List<String> DEVICES = List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7");
  private static final List<String> CONDITIONS = List.of("REFERENCE", "REDUCED");

  /** Impede instanciação do validador sem estado. */
  private PdeInputComparisonScenarioMatrixV1() {}

  /** Exige dezoito combinações, medições e sinais nas versões novas, preservando provas legadas. */
  static boolean valid(JsonNode result) {
    JsonNode scenarios = result.path("scenarios");
    String contract = result.path("fixtureContract").asText();
    boolean signaled = SIGNAL_CONTRACT.equals(contract);
    boolean measured = MEASURED_CONTRACT.equals(contract) || signaled;
    if ((!CONTRACT.equals(contract) && !measured)
        || (result.path("prototypeVersion").asText().startsWith("mira-private-candidate-v")
            && !measured)
        || (result.path("prototypeVersion").asText().startsWith("mira-private-candidate-v")
            && !List.of("mira-private-candidate-v1", "mira-private-candidate-v2")
                .contains(result.path("prototypeVersion").asText())
            && !signaled)
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
          || (measured && !validMeasurements(scenario))
          || (signaled && !validSignals(scenario))
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
            || (!measured
                && (reduced.path("manualFields").asInt(-1) < 1
                    || reference.path("manualFields").asInt(-1)
                        <= reduced.path("manualFields").asInt(-1)))
            || !reference.path("products").equals(reduced.path("products"))
            || !reference.path("routine").equals(reduced.path("routine"))) {
          return false;
        }
      }
    }
    return true;
  }

  /** Exige sinais realmente persistidos, sem simular continuidade em um percurso inseguro. */
  private static boolean validSignals(JsonNode scenario) {
    var events = new HashSet<String>();
    if (!scenario.path("events").isArray()) return false;
    scenario.path("events").forEach(event -> events.add(event.asText()));
    if (!events.contains("EXPERIENCE_STARTED")) return false;
    if ("SAFETY".equals(scenario.path("scenarioCode").asText()))
      return events.contains("SAFETY_LIMIT_BLOCKED")
          && !events.contains("PREFERRED_OVER_FREE")
          && !events.contains("CHECKOUT_STARTED");
    return events.containsAll(
        List.of("VALUE_MOMENT", "READY_RESULT_USED", "PREFERRED_OVER_FREE", "CHECKOUT_STARTED"));
  }

  /** Concilia mínimo, preenchimentos reais e latência monotônica sem reinterpretar o histórico. */
  private static boolean validMeasurements(JsonNode value) {
    int supplied = value.path("products").size();
    int productFields = value.path("filledProductFields").asInt(-1);
    int sourceFields = value.path("filledOptionalSourceFields").asInt(-1);
    int objectiveFields = value.path("editedObjectiveFields").asInt(-1);
    return !value.has("manualFields")
        && value.path("minimumRequiredProductFields").asInt(-1)
            == ("REFERENCE".equals(value.path("condition").asText()) ? 4 : 2)
        && value.path("providedProductCount").asInt(-1) == supplied
        && productFields == supplied * 2
        && productFields >= value.path("minimumRequiredProductFields").asInt(Integer.MAX_VALUE)
        && sourceFields >= 0
        && sourceFields <= supplied
        && objectiveFields >= 0
        && objectiveFields <= 1
        && value.path("actualFilledFields").asInt(-1)
            == productFields + sourceFields + objectiveFields
        && value.path("requiredObjectiveFields").asInt(-1) == 1
        && value.path("objectivePrefilled").isBoolean()
        && value.path("corrections").asInt(-1) >= 0
        && "MONOTONIC_WORKER".equals(value.path("measurementClock").asText())
        && "FIRST_VISIBLE_RESULT_OR_SAFE_BLOCK".equals(value.path("outcomeBoundary").asText())
        && value.path("scenarioCompletedSeconds").isNumber()
        && value.path("scenarioCompletedSeconds").asDouble(-1)
            >= value.path("resultReadySeconds").asDouble(601);
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
