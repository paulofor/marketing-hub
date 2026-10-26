package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Reproduz cobertura nominal e exige conexão emocional rastreável sem fabricar qualidade comercial.
 */
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

  /** Exercita desejos distintos, mantendo cada direção e suas mensagens no próprio produto. */
  @ParameterizedTest
  @ValueSource(strings = {"Divulgação para autônomos de beleza", "Estilo pessoal com suas roupas"})
  void acceptsEmotionalConnectionOnlyWithOwnDirection(String direction) {
    var proof = emotionalProof(json, direction);
    assertThatCode(() -> PersuasionEvidenceContract.validateFresh(proof, task(direction)))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> PersuasionEvidenceContract.validateFresh(proof, task("Outra direção")))
        .hasMessageContaining("Citação comercial ausente");
  }

  /** A fonte pode ser a Big Idea vinculada, mas nunca uma ideia ausente ou de outro contexto. */
  @Test
  void acceptsBoundBigIdeaAndRejectsMissingDirection() {
    var proof = emotionalProof(json, "Estilo pessoal");
    var connection = (ObjectNode) proof.path("emotionalConnection");
    connection.put("directionSourcePointer", "/productBigIdea/content/centralIdea");
    connection.put("directionSourceQuote", "Seu guarda-roupa pode ter mais a sua cara");
    var context = json.createObjectNode();
    context.putObject("marketStrategicContract").put("summary", "Estilo pessoal");
    context
        .putObject("productBigIdea")
        .putObject("content")
        .put("centralIdea", "Seu guarda-roupa pode ter mais a sua cara");
    assertThatCode(
            () ->
                PersuasionEvidenceContract.validateFresh(
                    proof, Map.of("taskId", 992L, "processContextJson", context.toString())))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () -> PersuasionEvidenceContract.validateFresh(proof, task("Estilo pessoal")))
        .hasMessageContaining("Citação comercial ausente");
  }

  /** Omisão, fonte falsa ou mensagens desvinculadas não podem parecer uma conexão concluída. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "missing",
        "blank",
        "wrongType",
        "quote",
        "pointer",
        "object",
        "opening",
        "proof",
        "step",
        "unknownVersion",
        "downgrade"
      })
  void rejectsDecorativeOrUnsupportedEmotionalMap(String defect) {
    var proof = emotionalProof(json, "Divulgação para autônomos de beleza");
    var connection = (ObjectNode) proof.path("emotionalConnection");
    switch (defect) {
      case "missing" -> proof.remove("emotionalConnection");
      case "blank" -> connection.put("beliefHypothesis", "  ");
      case "wrongType" -> connection.put("emotionalPayoff", true);
      case "quote" -> connection.put("directionSourceQuote", "Clientes garantidos");
      case "pointer" -> connection.put("directionSourcePointer", "/learningSalesCycle/summary");
      case "object" -> connection.put("directionSourcePointer", "/marketStrategicContract/");
      case "opening" -> connection.put("customerOpening", "Preencha os campos do sistema");
      case "proof" -> connection.put("proofBridge", "Prova não recebida");
      case "step" -> connection.put("easyNextStep", "Compre agora");
      case "unknownVersion" -> proof.put("contractVersion", "OTHER");
      case "downgrade" -> proof.put("contractVersion", "COMMERCIAL_PERSUASION_EVIDENCE_V1");
    }
    assertThatThrownBy(
            () ->
                PersuasionEvidenceContract.validateFresh(
                    proof, task("Divulgação para autônomos de beleza")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Preserva o custo e o recibo antigo sem permitir que nova inferência omita a evolução. */
  @Test
  void preservesHistoricalReceiptWithoutAllowingNewV1Output() {
    var historical = proof(json, "Kit para manicures");
    assertThatCode(
            () -> PersuasionEvidenceContract.validate(historical, task("Kit para manicures")))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () -> PersuasionEvidenceContract.validateFresh(historical, task("Kit para manicures")))
        .hasMessageContaining("conexão emocional V2");
  }

  /** Prepara uma hipótese sintética com mensagens correspondentes, sem afirmar reação humana. */
  static ObjectNode emotionalProof(ObjectMapper json, String direction) {
    ObjectNode proof = proof(json, direction);
    proof.put("contractVersion", "COMMERCIAL_PERSUASION_EVIDENCE_V2");
    var connection =
        proof
            .putObject("emotionalConnection")
            .put(
                "everydaySituation",
                "Ao preparar a próxima semana, escolher como quer se apresentar")
            .put(
                "beliefHypothesis",
                "Hipótese: minha apresentação deveria expressar o cuidado que tenho")
            .put("recognizedDesire", "Reconhecer o próprio gosto na apresentação")
            .put(
                "emotionalPayoff",
                "Possibilidade de sentir orgulho, sem resultado humano comprovado")
            .put("reframe", "Começar por uma amostra compatível com " + direction)
            .put("directionSourcePointer", "/marketStrategicContract/summary")
            .put("directionSourceQuote", direction)
            .put("customerOpening", "O cuidado que você tem pode aparecer na sua apresentação.")
            .put("proofBridge", "Veja esse cuidado na amostra de " + direction + ".")
            .put("easyNextStep", "Veja a amostra.")
            .put(
                "limitation",
                "Fixture sintética; crença e emoção são hipóteses, não demanda medida.");
    ((ObjectNode) proof.path("criteria").get(0))
        .put("customerMessage", connection.path("customerOpening").asText());
    ((ObjectNode) proof.path("criteria").get(1))
        .put("customerMessage", connection.path("easyNextStep").asText());
    ((ObjectNode) proof.path("criteria").get(2))
        .put("customerMessage", connection.path("proofBridge").asText());
    return proof;
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
