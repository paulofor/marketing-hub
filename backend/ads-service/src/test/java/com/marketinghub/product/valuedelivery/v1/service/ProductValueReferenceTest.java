package com.marketinghub.product.valuedelivery.v1.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: recusar referência ambígua ou embutida sem alterar bytes de respostas legadas.
 */
class ProductValueReferenceTest {
  private final ObjectMapper json = new ObjectMapper();
  private final String reference = "VALUE_SPECIFICATION:" + "a".repeat(64);

  /** Reconhece somente o campo explícito e os dois formatos escalares já emitidos. */
  @ParameterizedTest
  @ValueSource(strings = {"canonical", "literal", "prefix"})
  void acceptsDelimitedReferencesWithoutChangingResult(String format) {
    var result = json.createObjectNode();
    if ("canonical".equals(format)) result.put("specificationReference", reference);
    else
      result
          .putArray("acceptanceCriteria")
          .add(("prefix".equals(format) ? "specificationReference=" : "") + reference);
    String original = result.toString();
    ProductValueReference.require(result, reference);
    assertThat(result.toString()).isEqualTo(original);
  }

  /** Recusa ausência, hash divergente, campo arbitrário, prosa e múltiplas identidades. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "missing",
        "wrong",
        "embedded",
        "nested",
        "ambiguous",
        "trailing",
        "null",
        "case",
        "leading"
      })
  void rejectsNonCanonicalOrAmbiguousReferences(String format) {
    var result = json.createObjectNode();
    switch (format) {
      case "wrong" -> result.put("specificationReference", reference.replace('a', 'b'));
      case "embedded" -> result.putArray("acceptanceCriteria").add("Usar " + reference + " aqui");
      case "nested" -> result.putObject("unrelated").put("specificationReference", reference);
      case "ambiguous" -> {
        result.put("specificationReference", reference);
        result
            .putArray("acceptanceCriteria")
            .add("specificationReference=VALUE_SPECIFICATION:" + "b".repeat(64));
      }
      case "trailing" ->
          result
              .putArray("acceptanceCriteria")
              .add("specificationReference=" + reference + " e outra prova");
      case "null" -> result.putNull("specificationReference");
      case "case" -> result.put("specificationReference", reference.toLowerCase());
      case "leading" ->
          result.putArray("acceptanceCriteria").add(" specificationReference=" + reference);
      default -> result.putArray("acceptanceCriteria").add("Sem vínculo auditável");
    }
    assertThatThrownBy(() -> ProductValueReference.require(result, reference))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
