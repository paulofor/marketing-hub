package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reproduz cobertura nominal e exige fontes da mesma tarefa sem fabricar qualidade comercial. */
class PersuasionEvidenceContractTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Comprova os cinco pontos em Capella e em outra identidade sem regras por produto. */
  @ParameterizedTest
  @ValueSource(strings = {"Kit para manicures", "Roteiro para organização doméstica"})
  void acceptsOnlyTheReceivedSources(String product) {
    var input = task(product);
    var proof = proof(json, product);
    assertThatCode(() -> PersuasionEvidenceContract.validate(proof, input))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> PersuasionEvidenceContract.validate(proof, task("Outro produto")))
        .hasMessageContaining("Citação comercial ausente");
  }

  /** Fonte inexistente, externa ou com citação inventada não pode justificar um critério. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "quote",
        "pointer",
        "external",
        "duplicate",
        "missing",
        "metric",
        "margin",
        "message"
      })
  void rejectsUnsupportedEvidence(String defect) {
    ObjectNode proof = proof(json, "Kit para manicures");
    ObjectNode criterion = (ObjectNode) proof.path("criteria").get(0);
    switch (defect) {
      case "quote" -> criterion.put("sourceQuote", "Agenda cheia garantida em sete dias");
      case "pointer" -> criterion.put("sourcePointer", "/marketStrategicContract/missing");
      case "external" -> criterion.put("sourcePointer", "https://other.test/evidence");
      case "duplicate" ->
          ((ObjectNode) proof.path("criteria").get(1)).put("criterion", "RECOGNIZED_DESIRE");
      case "missing" ->
          ((com.fasterxml.jackson.databind.node.ArrayNode) proof.path("criteria")).remove(0);
      case "metric" -> criterion.put("metric", "NET_CONTRIBUTION_AFTER_ACQUISITION");
      case "margin" ->
          ((ObjectNode) proof.path("criteria").get(4)).put("customerMessage", "Nossa margem é 30%");
      case "message" -> criterion.putNull("customerMessage");
    }
    assertThatThrownBy(() -> PersuasionEvidenceContract.validate(proof, task("Kit para manicures")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Exige fonte por campo, evitando que um JSON completo aparente provar qualquer alegação. */
  @Test
  void rejectsObjectAsLiteralSource() {
    var proof = proof(json, "Kit para manicures");
    ((ObjectNode) proof.path("criteria").get(0)).put("sourcePointer", "/marketStrategicContract/");
    assertThatThrownBy(() -> PersuasionEvidenceContract.validate(proof, task("Kit para manicures")))
        .hasMessageContaining("Citação comercial ausente");
  }

  /** Gera cinco decisões sintéticas identificadas para exercitar o contrato, sem simular vendas. */
  static ObjectNode proof(ObjectMapper json, String source) {
    ObjectNode proof =
        json.createObjectNode().put("contractVersion", "COMMERCIAL_PERSUASION_EVIDENCE_V1");
    var criteria = proof.putArray("criteria");
    String[] codes = {
      "RECOGNIZED_DESIRE",
      "EASY_FIRST_STEP",
      "VALUE_BEFORE_COMMITMENT",
      "PAID_INCREMENT",
      "REPEAT_WITH_MARGIN"
    };
    String[] metrics = {
      "AD_CLICK_THROUGH_RATE",
      "SAMPLE_VIEW_RATE",
      "SAMPLE_TO_CHECKOUT_RATE",
      "NET_PURCHASE_CONVERSION_RATE",
      "NET_CONTRIBUTION_AFTER_ACQUISITION"
    };
    for (int i = 0; i < codes.length; i++) {
      var item =
          criteria
              .addObject()
              .put("criterion", codes[i])
              .put("decision", "Fixture: mostrar amostra compatível com o produto recebido")
              .put("sourcePointer", "/marketStrategicContract/summary")
              .put("sourceQuote", source)
              .put("metric", metrics[i])
              .put("limitation", "Cenário sintético; não comprova demanda, resultado nem receita");
      if (i == 4) item.putNull("customerMessage");
      else item.put("customerMessage", "Veja uma amostra de " + source);
    }
    return proof;
  }

  /** Produz o contexto oficial mínimo com uma fonte textual do produto sob teste. */
  private Map<String, Object> task(String product) {
    var context = json.createObjectNode();
    context.putObject("marketStrategicContract").put("summary", product);
    return Map.of("taskId", 1L, "processContextJson", context.toString());
  }
}
