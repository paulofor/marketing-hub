package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;

/**
 * Responsabilidade: validar provas persistidas da jornada contextual sem confundi-las com fixtures
 * estáticas.
 */
final class PdeContextualStyleScenarioMatrixV1 {
  static final String CONTRACT = "PDE_CONTEXTUAL_STYLE_EXECUTION_V1";
  private static final Set<String> CODES = Set.of("ADHERENT", "RECOVERY", "SAFETY");
  private static final Set<String> DEVICES = Set.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7");
  private static final Set<String> SIGNALS =
      Set.of(
          "EXPERIENCE_STARTED",
          "VALUE_MOMENT",
          "READY_RESULT_USED",
          "PREFERRED_OVER_FREE",
          "CHECKOUT_STARTED");

  /** Impede instância de um validador sem estado. */
  private PdeContextualStyleScenarioMatrixV1() {}

  /** Seleciona apenas o contrato explícito de regras contextuais executáveis. */
  static boolean supports(JsonNode result) {
    return CONTRACT.equals(result.path("runtimeContract").asText());
  }

  /** Exige nove sessões únicas, operações reais e fronteiras privadas em toda a matriz. */
  static boolean valid(JsonNode result) {
    if (!supports(result)
        || !"DETERMINISTIC_CONTEXTUAL_RULES".equals(result.path("generationMode").asText())
        || result.path("providerCalls").asInt(-1) != 0
        || result.path("modelFunctionalBehaviorClaimed").asBoolean(true)
        || result.path("inputComparisonApplicable").asBoolean(true)
        || !result.path("checks").path("sevenDayJourney").asBoolean(false)
        || !result.path("scenarios").isArray()
        || result.path("scenarios").size() != 9
        || result.path("prototypeVersion").asText().isBlank()) return false;
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

  /** Confere segurança, recuperação, jornada e sinais vinculados ao resultado da própria sessão. */
  private static boolean validScenario(JsonNode scenario, String code) {
    boolean safety = "SAFETY".equals(code);
    JsonNode operations = scenario.path("operations"), events = scenario.path("events");
    if (!"PASS".equals(scenario.path("status").asText())
        || !"AGENT_VALIDATION".equals(scenario.path("trafficClass").asText())
        || !scenario.path("mhInternalTest").asBoolean(false)
        || scenario.path("providerCalls").asInt(-1) != 0
        || scenario.path("humanEvidenceClaimed").asBoolean(true)
        || scenario.path("commercialEvidenceClaimed").asBoolean(true)
        || !privateEffects(scenario.path("sideEffects"))
        || !operations.isObject()
        || operations.size() != ("ADHERENT".equals(code) ? 7 : 1)
        || !events.isObject()) return false;
    Set<String> signals = new HashSet<>(), missions = new HashSet<>();
    events.fieldNames().forEachRemaining(signals::add);
    if (!signals.equals(safety ? Set.of("EXPERIENCE_STARTED") : SIGNALS)) return false;
    var iterator = operations.fields();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      JsonNode operation = entry.getValue(),
          response = operation.path("response"),
          decision = operation.path("decision");
      String expectedStatus = safety ? "BLOCKED" : "COMPLETED";
      if (entry.getKey().isBlank()
          || !entry.getKey().equals(response.path("requestId").asText())
          || !missions.add(operation.path("missionId").asText())
          || !operation.path("input").isObject()
          || !expectedStatus.equals(response.path("status").asText())
          || !expectedStatus.equals(decision.path("status").asText())
          || operation.path("providerCalls").asInt(-1) != 0
          || !operation.path("incrementalInferenceCostUsd").isNumber()
          || operation.path("incrementalInferenceCostUsd").decimalValue().signum() != 0
          || operation.path("startedAt").asText().isBlank()
          || operation.path("finishedAt").asText().isBlank()
          || !response.path("microActions").isArray()
          || (safety
              ? !response.path("microActions").isEmpty()
              : response.path("microActions").isEmpty())) return false;
    }
    if (safety)
      return scenario.path("safetyBlocked").asBoolean(false) && scenario.path("card").isNull();
    JsonNode card = scenario.path("card");
    String requestId = card.path("requestId").asText();
    if (!scenario.path("resumed").asBoolean(false)
        || requestId.isBlank()
        || !card.equals(operations.path(requestId).path("response"))) return false;
    for (String signal : SIGNALS) {
      if (!"EXPERIENCE_STARTED".equals(signal)
          && !requestId.equals(events.path(signal).path("requestId").asText())) return false;
    }
    return "ADHERENT".equals(code)
        ? scenario.path("journeyComplete").asBoolean(false)
        : scenario.path("recovered").asBoolean(false);
  }

  /** Recusa qualquer efeito comercial ou ausência de declaração explícita. */
  private static boolean privateEffects(JsonNode effects) {
    return !effects.path("paymentEnabled").asBoolean(true)
        && !effects.path("published").asBoolean(true)
        && !effects.path("campaignCreated").asBoolean(true)
        && effects.path("mediaSpendBrl").isNumber()
        && effects.path("mediaSpendBrl").decimalValue().signum() == 0;
  }
}
