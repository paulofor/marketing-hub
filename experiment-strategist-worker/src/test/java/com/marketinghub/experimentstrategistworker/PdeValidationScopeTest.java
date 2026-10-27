package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: testar o consumo do protocolo publicado pelo backend e impedir deriva. */
class PdeValidationScopeTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Reproduz o planejamento de Vega e outro produto pelo validador real do callback de Atena. */
  @ParameterizedTest
  @ValueSource(longs = {4L, 98004L})
  void rejectsImportedComparisonAndAcceptsCompatibleStrategy(long productId) throws Exception {
    var task = task(productId, false);
    var result = new PdeMarketStrategyBpmTaskConsumerTest().agentValidationResult();
    var strategy = result.withObject("/marketStrategicContract");
    strategy.put(
        "causalHypothesis", "Comparar REFERENCE e REDUCED para provar redução de entrada.");
    assertThatThrownBy(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(
                    result, task.path("sourceReference").asText(), false, task))
        .hasMessageContaining("fora do protocolo");
    strategy.put(
        "causalHypothesis", "Preferências distintas produzem sugestões úteis e diferentes.");
    assertThatCode(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(
                    result, task.path("sourceReference").asText(), false, task))
        .doesNotThrowAnyException();
    var context = (ObjectNode) json.readTree(task.path("processContextJson").asText());
    context
        .withObject("/learningSalesCycle/agentValidationExecution")
        .put("productId", productId + 1);
    task.put("processContextJson", context.toString());
    assertThatThrownBy(() -> PdeValidationScope.protocol(task))
        .hasMessageContaining("não corresponde");
  }

  /** Preserva a comparação documentada e permite relatar uma divergência sem aprovação falsa. */
  @Test
  void preservesValidComparisonAndAdjustmentsWithoutGrantingCapability() throws Exception {
    var result = new PdeMarketStrategyBpmTaskConsumerTest().agentValidationResult();
    result
        .withObject("/marketStrategicContract")
        .put("causalHypothesis", "REFERENCE versus REDUCED");
    assertThatCode(() -> PdeValidationScope.validate(result, task(98010L, true)))
        .doesNotThrowAnyException();
    result.put("decision", "ADJUST");
    assertThatCode(() -> PdeValidationScope.validate(result, task(98004L, false)))
        .doesNotThrowAnyException();
    assertThatCode(() -> PdeValidationScope.validate(result, null)).doesNotThrowAnyException();
  }

  /** Constrói transporte sintético usando os contratos reais do módulo produtor. */
  private ObjectNode task(long productId, boolean comparison) throws Exception {
    Path contracts = Path.of("../backend/ads-service/src/main/resources/contracts");
    var protocol =
        (ObjectNode)
            json.readTree(Files.readString(contracts.resolve("pde-agent-validation-plan-v1.json")));
    protocol.put("scopeContractVersion", "PDE_VALIDATION_SCOPE_V1");
    protocol
        .put("productId", productId)
        .put("cycleId", productId + 100)
        .put("experimentId", productId + 200);
    protocol.put("prototypeVersion", "fixture-v2").put("inputComparisonApplicable", comparison);
    var context = json.createObjectNode();
    var cycle = context.putObject("learningSalesCycle");
    cycle
        .put("productId", productId)
        .put("cycleId", productId + 100)
        .put("experimentId", productId + 200);
    cycle.put("productVersion", "fixture-v2");
    cycle.set("agentValidationExecution", protocol);
    if (comparison) {
      protocol.set(
          "documentedInputComparison",
          json.readTree(
              Files.readString(contracts.resolve("pde-documented-input-comparison-v3.json"))));
      cycle
          .putObject("implementedInput")
          .put("validationFixtureContract", "PDE_DOCUMENTED_INPUT_COMPARISON_V3");
    }
    var task = json.createObjectNode().put("sourceReference", "experiment:" + (productId + 200));
    task.put("processCode", "pde-commercial-plan-offer");
    task.put("processVersion", 11);
    task.putObject("processDefinition").put("versionNumber", 11);
    task.putObject("taskTarget").put("productId", productId);
    task.put("processContextJson", context.toString());
    return task;
  }
}
