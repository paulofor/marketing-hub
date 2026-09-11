package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Responsabilidade: verificar no executor real a entrada privada produzida pelo backend local. */
@EnabledIfEnvironmentVariable(named = "VEGA_IRIS_INPUT_FILE", matches = ".+")
class IrisCycleInputIntegrationTest {
  /** Aceita V3 íntegro com gate e rejeita a mesma entrada quando sua prontidão é removida. */
  @Test
  void acceptsBackendCycleInputAndRejectsMissingGate() throws Exception {
    var json = new ObjectMapper();
    Map<String, Object> input =
        json.readValue(Files.readString(Path.of(System.getenv("VEGA_IRIS_INPUT_FILE"))), Map.class);
    CommunicationAgentCodexRunner.validateInput(input);
    var context =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(input.get("processContextJson").toString());
    assertThat(context.path("marketStrategicContract").path("contractVersion").asText())
        .isEqualTo("MARKET_STRATEGY_V3");
    assertThat(
            context
                .path("communicationMaterializationContext")
                .path("validationPolicy")
                .path("mode")
                .asText())
        .isEqualTo("AGENT_VALIDATION");
    assertThat(
            context
                .path("communicationMaterializationContext")
                .path("publicationAuthorized")
                .asBoolean(true))
        .isFalse();
    ((com.fasterxml.jackson.databind.node.ObjectNode)
            context.path("communicationMaterializationContext"))
        .put("inputReadiness", "BLOCKED");
    input.put("processContextJson", context.toString());
    assertThatThrownBy(() -> CommunicationAgentCodexRunner.validateInput(input))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
