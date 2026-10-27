package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: impedir perda de provas e aprovações indevidas da jornada contextual no gate.
 */
class PdeContextualStyleScenarioMatrixV1Test {
  /**
   * Comprova a matriz e seu consumo pelo gate para identidades distintas, sem exceção por produto.
   */
  @Test
  void acceptsDistinctIdentitiesThroughActualGate() throws Exception {
    var method =
        PdeAgentValidationGateActivityExecutor.class.getDeclaredMethod(
            "validTechnicalScenarioMatrix", com.fasterxml.jackson.databind.JsonNode.class);
    method.setAccessible(true);
    var gate =
        org.mockito.Mockito.mock(
            PdeAgentValidationGateActivityExecutor.class, org.mockito.Mockito.CALLS_REAL_METHODS);
    for (String version :
        List.of("musa-pde-entry-v16-modelagem-internacional", "another-product-version-2")) {
      var result = fixture();
      result.put("prototypeVersion", version).put("productId", version.length());
      result.path("scenarios").forEach(row -> ((ObjectNode) row).put("prototypeVersion", version));
      assertThat(method.invoke(gate, result)).isEqualTo(true);
    }
  }

  /** Bloqueia sessão repetida, matriz incompleta e resultado sem operação correspondente. */
  @Test
  void rejectsLostEvidenceAndReusedSessions() throws Exception {
    var duplicate = fixture();
    row(duplicate, 1).put("evidenceId", row(duplicate, 0).path("evidenceId").asText());
    assertThat(PdeContextualStyleScenarioMatrixV1.valid(duplicate)).isFalse();
    var missing = fixture();
    ((ObjectNode) row(missing, 0).path("operations")).removeAll();
    assertThat(PdeContextualStyleScenarioMatrixV1.valid(missing)).isFalse();
    var unrelated = fixture();
    ((ObjectNode) row(unrelated, 0).path("card")).put("requestId", "another-session");
    assertThat(PdeContextualStyleScenarioMatrixV1.valid(unrelated)).isFalse();
    var lost = fixture();
    row(lost, 3).put("recovered", false);
    assertThat(PdeContextualStyleScenarioMatrixV1.valid(lost)).isFalse();
  }

  /** Rejeita avanço inseguro, inferência não declarada e falsa evidência comercial. */
  @Test
  void rejectsUnsafeOrPaidOutcomes() throws Exception {
    var unsafe = fixture();
    ((ObjectNode) row(unsafe, 6).path("events")).putObject("CHECKOUT_STARTED");
    assertThat(PdeContextualStyleScenarioMatrixV1.valid(unsafe)).isFalse();
    var paid = fixture();
    ((ObjectNode) row(paid, 0).path("operations").elements().next()).put("providerCalls", 1);
    assertThat(PdeContextualStyleScenarioMatrixV1.valid(paid)).isFalse();
    var claim = fixture();
    row(claim, 0).put("commercialEvidenceClaimed", true);
    assertThat(PdeContextualStyleScenarioMatrixV1.valid(claim)).isFalse();
  }

  /**
   * Aceita o relatório real da rodada local, quando fornecido, sem executar novamente o navegador.
   */
  @Test
  void acceptsActualBrowserReportWhenProvided() throws Exception {
    String path = System.getenv("VEGA_CONTEXTUAL_LOCAL_REPORT");
    org.junit.jupiter.api.Assumptions.assumeTrue(path != null && !path.isBlank());
    assertThat(
            PdeContextualStyleScenarioMatrixV1.valid(
                new ObjectMapper().readTree(Path.of(path).toFile())))
        .isTrue();
  }

  /** Carrega dados sintéticos de contrato, explicitamente distintos da evidência operacional. */
  private ObjectNode fixture() throws Exception {
    try (var input =
        getClass().getResourceAsStream("/vega-style/contextual-contract-fixture.json")) {
      return (ObjectNode) new ObjectMapper().readTree(input);
    }
  }

  /** Obtém uma linha editável da fixture isolada de cada teste. */
  private ObjectNode row(ObjectNode result, int index) {
    return (ObjectNode) result.path("scenarios").get(index);
  }
}
