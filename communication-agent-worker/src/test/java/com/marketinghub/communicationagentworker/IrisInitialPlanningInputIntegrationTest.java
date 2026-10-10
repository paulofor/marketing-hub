package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Responsabilidade: validar no executor a entrada inicial privada transportada pelo backend real.
 */
@EnabledIfEnvironmentVariable(named = "IRIS_INITIAL_INPUT_FILE", matches = ".+")
class IrisInitialPlanningInputIntegrationTest {
  /** Aceita prova técnica com planejamento próprio e rejeita prontidão perdida sem inferência. */
  @Test
  void acceptsScopedSoftwareProofWithoutCommercialApproval() throws Exception {
    var json = new ObjectMapper();
    Map<String, Object> input =
        json.readValue(
            Files.readString(Path.of(System.getenv("IRIS_INITIAL_INPUT_FILE"))), Map.class);
    CommunicationAgentCodexRunner.validateInput(input);
    var frozen = (ObjectNode) json.readTree(input.get("processContextJson").toString());
    var communication = (ObjectNode) frozen.path("communicationMaterializationContext");
    assertThat(frozen.path("marketStrategicContract").path("contractVersion").asText())
        .isEqualTo("MARKET_STRATEGY_V4");
    assertThat(communication.path("mode").asText()).isEqualTo("INITIAL_EXPERIMENT_PRIVATE");
    assertThat(communication.path("productProofScope").asText())
        .isEqualTo("PRIVATE_SOFTWARE_VERSION_ONLY");
    assertThat(communication.path("approvedVisualArtifacts")).isNotEmpty();
    assertThat(communication.path("prototypeVersion").asText()).isNotBlank();
    assertThat(communication.path("paymentEnabled").asBoolean(true)).isFalse();
    assertThat(communication.path("publicationAuthorized").asBoolean(true)).isFalse();
    assertThat(communication.path("externalMediaSpendAuthorized").asBoolean(true)).isFalse();
    communication.put("inputReadiness", "BLOCKED");
    input.put("processContextJson", frozen.toString());
    assertThatThrownBy(() -> CommunicationAgentCodexRunner.validateInput(input))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
