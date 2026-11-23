package com.marketinghub.product.valuedelivery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/** Responsabilidade: conferir a referência explícita e seus formatos legados delimitados. */
final class ProductValueReference {
  /** Impede instanciação do validador sem estado. */
  private ProductValueReference() {}

  /**
   * Aceita campo canônico ou escalar inteiro nos critérios legados; nunca busca em prosa ou campos
   * arbitrários. Referências duplicadas idênticas são compatíveis; divergência bloqueia.
   */
  static void require(JsonNode result, String expected) {
    List<String> references = new ArrayList<>();
    if (result.has("specificationReference")) {
      JsonNode explicit = result.path("specificationReference");
      if (!explicit.isTextual()) reject();
      references.add(explicit.asText());
    }
    for (JsonNode criterion : result.path("acceptanceCriteria")) {
      if (!criterion.isTextual()) continue;
      String value = criterion.asText();
      if (value.startsWith("specificationReference=")) {
        references.add(value.substring("specificationReference=".length()));
      } else if (value.startsWith("VALUE_SPECIFICATION:")) {
        references.add(value);
      }
    }
    if (references.isEmpty() || references.stream().anyMatch(value -> !expected.equals(value)))
      reject();
  }

  /** Recusa ausência ou divergência sem fabricar uma referência para a resposta. */
  private static void reject() {
    throw new IllegalArgumentException(
        "O resultado não identifica univocamente a especificação atual; preserve a saída e corrija o contrato.");
  }
}
