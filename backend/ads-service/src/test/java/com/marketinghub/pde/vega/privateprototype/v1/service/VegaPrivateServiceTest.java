package com.marketinghub.pde.vega.privateprototype.v1.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: impedir que resultado incompleto, manipulável ou dependente de compra seja
 * publicado.
 */
class VegaPrivateServiceTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Exige todos os campos, restrição de itens e ausência de metadados arbitrários. */
  @Test
  void validatesFunctionalCard() throws Exception {
    var card =
        json.readTree(
            "{\"cardId\":\"1\",\"action\":\"Acomode a camisa\",\"application\":\"Alinhe o tecido com conforto\",\"occasion\":\"Almoço\",\"selfAssessmentPrompt\":\"Ficou confortável?\",\"usesOnlyAvailableItems\":true}");
    assertThat(VegaPrivateService.validCard(card)).isTrue();
    var missing = card.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) missing).remove("application");
    assertThat(VegaPrivateService.validCard(missing)).isFalse();
    var purchase = card.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) purchase)
        .put("action", "Compre outra camisa");
    assertThat(VegaPrivateService.validCard(purchase)).isFalse();
    var extra = card.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) extra).put("debug", "internal");
    assertThat(VegaPrivateService.validCard(extra)).isFalse();
  }
}
