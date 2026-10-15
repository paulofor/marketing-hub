package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Testa a fronteira de aceite textual com pareceres simulados, sem alegar persuasão medida. */
class VideoCopyReviewContractTest {
  static final String NARRATION =
      """
      O atendimento ficou lindo. Mas, na hora de postar, faltou o que escrever?
      Você informa seu serviço e recebe um kit com a identidade do seu negócio.
      Veja este post e a legenda feitos para combinar. Confira a amostra do kit.
      """;
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita evidência literal e reutiliza o mesmo contrato com identidades distintas. */
  @ParameterizedTest
  @ValueSource(longs = {69, 910069})
  void acceptsEvidenceForAnyProduct(long assetId) {
    ObjectNode media = json.createObjectNode().put("assetId", assetId).put("narration", NARRATION);
    assertThatCode(() -> VideoCopyReviewContract.validate(media, review(json), true))
        .doesNotThrowAnyException();
    ObjectNode reloaded = json.convertValue(review(json), ObjectNode.class);
    assertThatCode(() -> VideoCopyReviewContract.validate(media, reloaded, true))
        .doesNotThrowAnyException();
  }

  /** O diagnóstico descritivo impede aprovação mesmo se todos os números estiverem altos. */
  @Test
  void refusesDescriptiveApprovalButAllowsActionableAdjustment() {
    ObjectNode review = review(json).put("mode", "DESCRIPTIVE");
    assertThatThrownBy(() -> VideoCopyReviewContract.validate(media(), review, true))
        .hasMessageContaining("descritiva");
    assertThatCode(() -> VideoCopyReviewContract.validate(media(), review, false))
        .doesNotThrowAnyException();
  }

  /** Nenhum critério textual fraco pode ser compensado por pontuações visuais altas. */
  @ParameterizedTest
  @ValueSource(
      strings = {"recognition", "humanConnection", "benefit", "simplicity", "proofBridge", "cta"})
  void refusesLowCriterion(String name) {
    ObjectNode review = review(json);
    ((ObjectNode) review.path(name)).put("score", 2);
    assertThatThrownBy(() -> VideoCopyReviewContract.validate(media(), review, true))
        .hasMessageContaining(name);
    assertThatCode(() -> VideoCopyReviewContract.validate(media(), review, false))
        .doesNotThrowAnyException();
  }

  /** Não permite inventar fala de outra edição para atribuir benefício à edição atual. */
  @Test
  void refusesFabricatedQuoteAndUnknownNarration() {
    ObjectNode review = review(json);
    ((ObjectNode) review.path("benefit")).put("quote", "Ganhe dez clientes amanhã.");
    assertThatThrownBy(() -> VideoCopyReviewContract.validate(media(), review, false))
        .hasMessageContaining("fora da narração");
    assertThatThrownBy(
            () -> VideoCopyReviewContract.validate(json.createObjectNode(), review(json), true))
        .hasMessageContaining("narração comprovada");
  }

  /** Reproduz as falas históricas sem tratá-las como nova evidência ou resultado comercial. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "Arte e texto combinam. Copie a legenda. Baixe seus arquivos.",
        "Mira organiza suas informações, com fontes e limites. Veja um exemplo real. Conheça Mira."
      })
  void rejectsDescriptiveHistoricalNarrationWithVisualApproval(String narration) {
    ObjectNode review = review(json).put("mode", "DESCRIPTIVE");
    for (String key :
        List.of("recognition", "humanConnection", "benefit", "simplicity", "proofBridge", "cta"))
      ((ObjectNode) review.path(key))
          .put("score", 2)
          .put("quote", narration)
          .put(
              "assessment",
              "Fixture de ajuste baseada no feedback do operador; lista de funções sem conexão suficiente.");
    var source = json.createObjectNode().put("narration", narration);
    assertThatThrownBy(() -> VideoCopyReviewContract.validate(source, review, true))
        .hasMessageContaining("descritiva");
    assertThatCode(() -> VideoCopyReviewContract.validate(source, review, false))
        .doesNotThrowAnyException();
  }

  /** Tipos inválidos e citações vazias não podem aparentar evidência textual suficiente. */
  @Test
  void refusesNullNarrationAndFabricatedEvidenceTypes() {
    assertThatThrownBy(
            () ->
                VideoCopyReviewContract.validate(
                    json.createObjectNode().putNull("narration"), review(json), true))
        .hasMessageContaining("narração comprovada");
    ObjectNode review = review(json);
    ((ObjectNode) review.path("benefit")).put("quote", "");
    assertThatThrownBy(() -> VideoCopyReviewContract.validate(media(), review, true))
        .hasMessageContaining("Citação");
    ((ObjectNode) review.path("benefit")).put("quote", 123);
    assertThatThrownBy(() -> VideoCopyReviewContract.validate(media(), review, false))
        .hasMessageContaining("incompleto");
  }

  /** Ausência de voz deve ser registrada sem obrigar a fabricar citação nem repetir produção. */
  @Test
  void allowsBlockedReviewWithoutInventedQuotes() {
    ObjectNode review = review(json).put("mode", "UNVERIFIABLE");
    for (String key :
        List.of("recognition", "humanConnection", "benefit", "simplicity", "proofBridge", "cta"))
      ((ObjectNode) review.path(key))
          .put("score", 0)
          .put("quote", "")
          .put("assessment", "Narração não recebida; recuperar a fonte antes de revisar.");
    assertThatCode(() -> VideoCopyReviewContract.validate(json.createObjectNode(), review, false))
        .doesNotThrowAnyException();
  }

  /** A demonstração paga precisa de benefício sustentado, sem exigir preço em todo anúncio. */
  @Test
  void enforcesPaidIncrementOnlyWhenApplicable() {
    ObjectNode review = review(json);
    ObjectNode paid = (ObjectNode) review.path("paidContinuity");
    paid.put("applicable", true)
        .put("score", 2)
        .put("quote", "recebe um kit")
        .put("assessment", "Entrega citada sem ganho da continuidade; reescrever.");
    assertThatThrownBy(() -> VideoCopyReviewContract.validate(media(), review, true))
        .hasMessageContaining("paidContinuity");
    paid.put("score", 3);
    assertThatCode(() -> VideoCopyReviewContract.validate(media(), review, true))
        .doesNotThrowAnyException();
    paid.put("applicable", false);
    assertThatThrownBy(() -> VideoCopyReviewContract.validate(media(), review, true))
        .hasMessageContaining("não aplicável");
  }

  /** O consumidor recebe a nova rubrica junto da narração real, sem executar modelo externo. */
  @Test
  void composesExecutableReviewPromptAndStrictSchema() throws Exception {
    var consumer =
        new CustomerBpmTaskConsumer(
            "http://backend.test", "codex", "gpt-5.6-sol", "max", "/unused", "/unused", json);
    var task =
        Map.<String, Object>of(
            "processCode",
            "creative-production-approval",
            "activityId",
            "customer",
            "taskTarget",
            Map.of(
                "pdeContext", Map.of("finalVideoReview", List.of(Map.of("narration", NARRATION)))));
    String prompt = consumer.prompt(task, List.of());
    assertThat(prompt)
        .contains(
            "VIDEO_COPY_REVIEW_V1",
            "VIDEO_NARRATIVE_REVIEW_V1",
            "evidenceId",
            "Mais cortes não significam mais interesse",
            "Visual bonito não compensa texto frio",
            "DESCRIPTIVE exige ADJUST",
            "O atendimento ficou lindo");
    try (var input =
        getClass().getResourceAsStream("/prompts/bpm/v3/creative-customer-review-schema.json")) {
      var schema = json.readTree(input).at("/properties/videoAudit/items");
      assertThat(schema.path("required").toString()).contains("copyReview", "narrativeReview");
      assertThat(schema.at("/properties/narrativeReview/required").toString())
          .contains("moments", "causalProgression", "payoff");
      assertThat(schema.at("/properties/copyReview/additionalProperties").asBoolean(true))
          .isFalse();
      for (String key :
          List.of(
              "recognition",
              "humanConnection",
              "benefit",
              "simplicity",
              "proofBridge",
              "cta",
              "paidContinuity"))
        assertThat(schema.at("/properties/copyReview/required").toString()).contains(key);
    }
  }

  /** Cria a entrada textual simulada, identificada como prova de teste e não cliente real. */
  private ObjectNode media() {
    return json.createObjectNode().put("narration", NARRATION);
  }

  /** Monta escores sintéticos para verificar o contrato; eles não são pareceres reais de Psique. */
  static ObjectNode review(ObjectMapper json) {
    ObjectNode result =
        json.createObjectNode()
            .put("contractVersion", "VIDEO_COPY_REVIEW_V1")
            .put("mode", "PERSUASIVE");
    Map<String, String> quotes =
        Map.of(
            "recognition", "na hora de postar, faltou o que escrever?",
            "humanConnection", "O atendimento ficou lindo.",
            "benefit", "recebe um kit com a identidade do seu negócio.",
            "simplicity", "Você informa seu serviço",
            "proofBridge", "Veja este post e a legenda feitos para combinar.",
            "cta", "Confira a amostra do kit.");
    quotes.forEach(
        (key, quote) ->
            result
                .putObject(key)
                .put("score", 4)
                .put("quote", quote)
                .put(
                    "assessment",
                    "Avaliação simulada para testar correspondência literal; não mede conversão."));
    result
        .putObject("paidContinuity")
        .put("applicable", false)
        .put("score", 0)
        .put("quote", "")
        .put(
            "assessment",
            "Anúncio convida à amostra; oferta completa fica na demonstração/destino.");
    return result;
  }
}
