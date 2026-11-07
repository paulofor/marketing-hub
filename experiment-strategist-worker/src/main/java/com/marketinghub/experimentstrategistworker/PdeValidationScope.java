package com.marketinghub.experimentstrategistworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.validationscope.v1.PdeValidationScopePolicy;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsabilidade: limitar a homologação de Atena à capacidade da candidata vinculada à tarefa.
 */
final class PdeValidationScope {
  private static final Logger log = LoggerFactory.getLogger(PdeValidationScope.class);
  private static final ObjectMapper JSON = new ObjectMapper();

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
          || protocol.path("experimentId").asLong(-1)
              != task.path("taskTarget").path("experimentId").asLong(-2)
          || protocol.path("cycleId").asLong(-1) != cycle.path("cycleId").asLong(-2)
          || protocol.path("experimentId").asLong(-1) != cycle.path("experimentId").asLong(-2)
          || !protocol
              .path("prototypeVersion")
              .asText()
              .equals(cycle.path("productVersion").asText())
          || protocol.path("prototypeVersion").asText().isBlank()
          || !task.path("sourceReference")
              .asText()
              .equals("experiment:" + protocol.path("experimentId").asLong(-1)))
        throw new IllegalArgumentException(
            "Protocolo de homologação não corresponde ao alvo da tarefa.");
      boolean applicable = protocol.path("inputComparisonApplicable").asBoolean();
      if (applicable
              && (!"PDE_IMPLEMENTED_INPUT_V1"
                      .equals(cycle.path("implementedInput").path("contractVersion").asText())
                  || !"PDE_DOCUMENTED_INPUT_COMPARISON_V3"
                      .equals(
                          cycle.path("implementedInput").path("validationFixtureContract").asText())
                  || !"PDE_DOCUMENTED_INPUT_COMPARISON_V3"
                      .equals(
                          protocol
                              .path("documentedInputComparison")
                              .path("contractVersion")
                              .asText())
                  || !PdeValidationScopePolicy.sameCandidate(
                      cycle.path("implementedInput"), protocol))
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

  /** Confere requisitos estruturados e históricos sem conceder comparação ou matriz por menção. */
  static void validate(JsonNode result, JsonNode task) {
    var protocol = protocol(task);
    if (protocol.isMissingNode() || !"APPROVE".equals(result.path("decision").asText())) return;
    PdeValidationScopePolicy.violation(result, protocol)
        .ifPresent(
            issue -> {
              log.warn(
                  "Atena recusou escopo taskId={} sourceReference={} code={} path={}",
                  task.path("taskId"),
                  task.path("sourceReference").asText(),
                  issue.code(),
                  issue.path());
              throw new IllegalArgumentException(issue.message());
            });
  }
}
