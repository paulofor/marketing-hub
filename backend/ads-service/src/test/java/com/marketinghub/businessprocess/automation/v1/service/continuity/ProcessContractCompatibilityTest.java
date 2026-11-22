package com.marketinghub.businessprocess.automation.v1.service.continuity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir reaproveitamento automático quando o contrato da entrega mudou. */
class ProcessContractCompatibilityTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Preserva contratos idênticos mesmo com outra ordem de propriedades. */
  @Test
  void acceptsEquivalentContracts() throws Exception {
    assertThat(
            ProcessContractCompatibility.differences(
                json.readTree("{\"id\":\"journey\",\"description\":\"Aceite original\"}"),
                json.readTree("{\"description\":\"Aceite original\",\"id\":\"journey\"}")))
        .isEmpty();
  }

  /**
   * Reproduz o acréscimo de requisito da versão publicada, sem exceção por produto ou atividade.
   */
  @Test
  void requiresRevalidationForChangedAcceptance() throws Exception {
    for (String activity : new String[] {"journey", "access", "other-product-output"}) {
      var source =
          json.createObjectNode().put("id", activity).put("description", "Aceite original");
      var target = source.deepCopy().put("description", "Aceite original. Novo requisito.");
      assertThat(ProcessContractCompatibility.differences(source, target))
          .containsExactly("/description");
    }
  }

  /** Expõe remoções e mudanças de integração em vez de aceitar apenas o mesmo nome. */
  @Test
  void identifiesMissingAndChangedContracts() throws Exception {
    assertThat(
            ProcessContractCompatibility.differences(
                json.readTree("{\"executor\":{\"schema\":\"v1\"},\"gate\":true}"),
                json.readTree("{\"executor\":{\"schema\":\"v2\"}}")))
        .containsExactly("/executor/schema", "/gate");
  }

  /** Contratos ausentes nunca comprovam compatibilidade, mesmo quando ambos estão vazios. */
  @Test
  void rejectsAbsentContracts() {
    assertThat(ProcessContractCompatibility.differences(null, null)).containsExactly("/");
    assertThat(
            ProcessContractCompatibility.differences(
                json.createObjectNode(), json.createObjectNode()))
        .containsExactly("/");
  }
}
