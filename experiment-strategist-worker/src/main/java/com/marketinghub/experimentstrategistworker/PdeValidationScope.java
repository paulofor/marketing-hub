package com.marketinghub.experimentstrategistworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: impedir que Atena imponha uma comparação pertencente a outro harness. */
final class PdeValidationScope {
  private static final Logger log = LoggerFactory.getLogger(PdeValidationScope.class);
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Pattern COMPARISON =
      Pattern.compile("\\b(?:REFERENCE|REDUCED|PDE_DOCUMENTED_INPUT_COMPARISON_V[1-3])\\b");

  /** Impede instâncias de uma validação sem estado operacional. */
  private PdeValidationScope() {}

  /** Confere a identidade antes da inferência, preservando tarefas antigas sem este contrato. */
  static JsonNode protocol(JsonNode task) {
    if (task == null) return JSON.missingNode();
    try {
      var cycle =
          JSON.readTree(task.path("processContextJson").asText("{}")).path("learningSalesCycle");
      var protocol = cycle.path("agentValidationExecution");
      if (!protocol.has("scopeContractVersion")) return JSON.missingNode();
      if (!"PDE_VALIDATION_SCOPE_V1".equals(protocol.path("scopeContractVersion").asText())
          || !protocol.path("inputComparisonApplicable").isBoolean()
          || protocol.path("productId").asLong(-1) != cycle.path("productId").asLong(-2)
          || protocol.path("productId").asLong(-1)
              != task.path("taskTarget").path("productId").asLong(-2)
          || protocol.path("cycleId").asLong(-1) != cycle.path("cycleId").asLong(-2)
          || protocol.path("experimentId").asLong(-1) != cycle.path("experimentId").asLong(-2)
          || !protocol
              .path("prototypeVersion")
              .asText()
              .equals(cycle.path("productVersion").asText())
          || !task.path("sourceReference")
              .asText()
              .equals("experiment:" + protocol.path("experimentId").asLong(-1)))
        throw new IllegalArgumentException(
            "Protocolo de homologação não corresponde ao alvo da tarefa.");
      boolean applicable = protocol.path("inputComparisonApplicable").asBoolean();
      if (applicable
              && (!"PDE_DOCUMENTED_INPUT_COMPARISON_V3"
                      .equals(
                          cycle.path("implementedInput").path("validationFixtureContract").asText())
                  || !"PDE_DOCUMENTED_INPUT_COMPARISON_V3"
                      .equals(
                          protocol
                              .path("documentedInputComparison")
                              .path("contractVersion")
                              .asText()))
          || !applicable && protocol.has("documentedInputComparison"))
        throw new IllegalArgumentException(
            "Comparação documental sem capacidade compatível da candidata.");
      return protocol;
    } catch (IOException ex) {
      log.error(
          "Falha no protocolo de Atena sourceReference={}",
          task.path("sourceReference").asText(),
          ex);
      throw new IllegalArgumentException("Protocolo de homologação ilegível.", ex);
    }
  }

  /** Recusa aprovação com obrigação comparativa que a candidata não recebeu do backend. */
  static void validate(JsonNode result, JsonNode task) {
    var protocol = protocol(task);
    if (protocol.isMissingNode()
        || protocol.path("inputComparisonApplicable").asBoolean()
        || !"APPROVE".equals(result.path("decision").asText())) return;
    var strategy = result.path("marketStrategicContract");
    if (COMPARISON.matcher(strategy.path("causalHypothesis").asText()).find()
        || COMPARISON.matcher(strategy.path("continueCriteria").asText()).find()
        || COMPARISON.matcher(strategy.path("agentValidationPlan").toString()).find())
      throw new IllegalArgumentException(
          "Atena exigiu comparação fora do protocolo da candidata; alinhe a hipótese à base disponível.");
  }
}
