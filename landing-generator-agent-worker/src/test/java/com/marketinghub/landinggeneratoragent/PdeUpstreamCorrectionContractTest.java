package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Responsabilidade: prevenir devolução ambígua sem impedir replay de respostas já produzidas. */
class PdeUpstreamCorrectionContractTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita origem completa e recusa aprovação, destino próprio ou aceite ausente. */
  @Test
  void requiresActionableReturnAndKeepsLegacyReplay() throws Exception {
    var result =
        json.readTree(
            """
      {"decision":"ADJUST","upstreamCorrection":{"contractVersion":"PROCESS_UPSTREAM_CORRECTION_V1",
      "targetActivityId":"marketStrategy","reason":"Entrada não preservada",
      "acceptanceCriterion":"Registrar a especialidade existente no contrato"}}
      """);
    assertThatCode(() -> PdeUpstreamCorrectionContract.validate(result)).doesNotThrowAnyException();
    var node = (com.fasterxml.jackson.databind.node.ObjectNode) result;
    node.put("decision", "APPROVE");
    assertThatThrownBy(() -> PdeUpstreamCorrectionContract.validate(result))
        .hasMessageContaining("Retorno à origem");
    node.put("decision", "ADJUST");
    var request = (com.fasterxml.jackson.databind.node.ObjectNode) node.path("upstreamCorrection");
    request.put("targetActivityId", "productArchitecture");
    assertThatThrownBy(() -> PdeUpstreamCorrectionContract.validate(result))
        .isInstanceOf(IllegalArgumentException.class);
    request.put("targetActivityId", "economics");
    request.remove("acceptanceCriterion");
    assertThatThrownBy(() -> PdeUpstreamCorrectionContract.validate(result))
        .isInstanceOf(IllegalArgumentException.class);
    node.remove("upstreamCorrection");
    assertThatCode(() -> PdeUpstreamCorrectionContract.validate(result)).doesNotThrowAnyException();
  }
}
