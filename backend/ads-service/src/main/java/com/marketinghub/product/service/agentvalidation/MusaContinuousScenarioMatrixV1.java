package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;

/**
 * Valida provas reais de orientação, repertório e adaptação contínua sem exigir a jornada antiga.
 */
final class MusaContinuousScenarioMatrixV1 {
  static final String CONTRACT = "MUSA_CONTINUOUS_STYLE_EXECUTION_V1";
  private static final Set<String> CODES = Set.of("ADHERENT", "RECOVERY", "SAFETY");
  private static final Set<String> DEVICES = Set.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7");
  private static final Set<String> SIGNALS =
      Set.of(
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED");

  /** Impede instância do validador sem estado. */
  private MusaContinuousScenarioMatrixV1() {}

  /** Seleciona exclusivamente o contrato contínuo versionado. */
  static boolean supports(JsonNode result) {
    return CONTRACT.equals(result.path("runtimeContract").asText());
  }

  /** Exige nove sessões distintas e provas vinculadas às operações da mesma versão. */
  static boolean valid(JsonNode result) {
    if (!supports(result)
        || !"musa-consultor-pwa-v1".equals(result.path("prototypeVersion").asText())
        || !"DETERMINISTIC_CONTEXTUAL_RULES".equals(result.path("generationMode").asText())
        || result.path("providerCalls").asInt(-1) != 0
        || result.path("modelFunctionalBehaviorClaimed").asBoolean(true)
        || result.path("inputComparisonApplicable").asBoolean(true)
        || !result.path("checks").path("continuousJourney").asBoolean(false)
        || !result.path("scenarios").isArray()
        || result.path("scenarios").size() != 9) return false;
    Set<String> combinations = new HashSet<>(), sessions = new HashSet<>();
    for (JsonNode scenario : result.path("scenarios")) {
      String code = scenario.path("scenarioCode").asText(),
          device = scenario.path("deviceProfile").asText();
      if (!CODES.contains(code)
          || !DEVICES.contains(device)
          || !combinations.add(code + "@" + device)
          || scenario.path("evidenceId").asText().isBlank()
          || !sessions.add(scenario.path("evidenceId").asText())
          || !result.path("prototypeVersion").equals(scenario.path("prototypeVersion"))
          || !validScenario(scenario, code)) return false;
    }
    return combinations.size() == 9;
  }

  /** Confere saída, salvamento, origem, retomada e sinais sem inferir comportamento humano. */
  private static boolean validScenario(JsonNode scenario, String code) {
    boolean safety = "SAFETY".equals(code);
    JsonNode ops = scenario.path("operations"),
        choices = scenario.path("choices"),
        events = scenario.path("events");
    if (!"PASS".equals(scenario.path("status").asText())
        || !"AGENT_VALIDATION".equals(scenario.path("trafficClass").asText())
        || !scenario.path("mhInternalTest").asBoolean(false)
        || scenario.path("providerCalls").asInt(-1) != 0
        || scenario.path("humanEvidenceClaimed").asBoolean(true)
        || scenario.path("commercialEvidenceClaimed").asBoolean(true)
        || !privateEffects(scenario.path("sideEffects"))
        || !ops.isObject()
        || ops.size() != (safety ? 1 : 2)
        || !choices.isObject()
        || choices.size() != (safety ? 0 : "ADHERENT".equals(code) ? 2 : 1)
        || !events.isObject()) return false;
    Set<String> signals = new HashSet<>();
    events.fieldNames().forEachRemaining(signals::add);
    if (!signals.equals(safety ? Set.of("EXPERIENCE_STARTED") : SIGNALS)) return false;
    int completed = 0, blocked = 0;
    var entries = ops.fields();
    while (entries.hasNext()) {
      var entry = entries.next();
      JsonNode op = entry.getValue();
      if (!entry.getKey().equals(op.path("operationId").asText())
          || !entry.getKey().equals(op.path("request").path("operationKey").asText())
          || !op.path("answers").isObject()
          || !op.path("request").isObject()
          || op.path("providerCalls").asInt(-1) != 0
          || !op.path("incrementalInferenceCostUsd").isNumber()
          || op.path("incrementalInferenceCostUsd").decimalValue().signum() != 0
          || op.path("startedAt").asText().isBlank()
          || op.path("finishedAt").asText().isBlank()
          || !events.path("EXPERIENCE_STARTED").has(entry.getKey())) return false;
      if ("COMPLETED".equals(op.path("status").asText())) {
        completed++;
        JsonNode choice = choices.path(entry.getKey());
        if (!choice.isObject()
            || op.path("action").asText().isBlank()
            || op.path("reason").asText().isBlank()
            || choice.path("savedAt").asText().isBlank()
            || !op.equals(
                ((com.fasterxml.jackson.databind.node.ObjectNode) choice.deepCopy())
                    .without("savedAt"))) return false;
      } else if ("BLOCKED".equals(op.path("status").asText())) {
        blocked++;
        if (!op.path("action").asText().isEmpty() || choices.has(entry.getKey())) return false;
      } else return false;
    }
    if (safety)
      return completed == 0
          && blocked == 1
          && scenario.path("card").isNull()
          && scenario.path("safetyBlocked").asBoolean(false)
          && scenario.path("crossOwnerDenied").asBoolean(false)
          && scenario.path("crossOwnerStatus").asInt() == 409;
    JsonNode card = scenario.path("card");
    String id = card.path("operationId").asText();
    if (id.isBlank() || !card.equals(ops.path(id)) || !scenario.path("resumed").asBoolean(false))
      return false;
    for (String signal : SIGNALS) if (!events.path(signal).has(id)) return false;
    if ("RECOVERY".equals(code))
      return completed == 1
          && blocked == 1
          && scenario.path("recovered").asBoolean(false)
          && scenario.path("responseLost").asBoolean(false)
          && scenario.path("repeatedWithoutDuplicate").asBoolean(false)
          && id.equals(scenario.path("replayOperationId").asText());
    JsonNode original = choices.path(card.path("parentChoiceId").asText());
    return completed == 2
        && blocked == 0
        && scenario.path("continuousJourney").asBoolean(false)
        && !original.isMissingNode()
        && original.path("parentChoiceId").asText().isEmpty()
        && !original.path("answers").path("occasion").equals(card.path("answers").path("occasion"))
        && original
            .path("answers")
            .path("existingSelection")
            .equals(card.path("answers").path("existingSelection"))
        && !original.path("action").equals(card.path("action"))
        && !card.path("continuity").asText().isBlank();
  }

  /** Recusa ausência de declaração e qualquer efeito comercial. */
  private static boolean privateEffects(JsonNode effects) {
    return !effects.path("paymentEnabled").asBoolean(true)
        && !effects.path("published").asBoolean(true)
        && !effects.path("campaignCreated").asBoolean(true)
        && effects.path("mediaSpendBrl").isNumber()
        && effects.path("mediaSpendBrl").decimalValue().signum() == 0;
  }
}
