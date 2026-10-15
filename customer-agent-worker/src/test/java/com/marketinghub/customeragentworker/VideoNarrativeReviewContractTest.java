package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Verifica evidência narrativa simulada, identidade e recuperação sem executar modelo pago. */
class VideoNarrativeReviewContractTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  /** Aceita a mesma regra em produtos distintos e após serialização para replay. */
  @ParameterizedTest
  @ValueSource(longs = {69, 910069})
  void acceptsProgressionWithReceivedFramesAndReplay(long assetId) throws Exception {
    var review = json.readTree(review(json).toString());
    var frames =
        json.readValue(
            json.writeValueAsString(evidence(assetId)),
            BpmVisualEvidenceBackendClient.UploadedVisualEvidence[].class);
    assertThatCode(
            () ->
                VideoNarrativeReviewContract.validate(
                    media(assetId), review, true, List.of(frames)))
        .doesNotThrowAnyException();
  }

  /** Nota estética alta não dispensa a avaliação narrativa nem aprova montagem decorativa. */
  @Test
  void rejectsMissingOrDecorativeNarrativeButPreservesAdjustment() {
    assertThatThrownBy(
            () ->
                VideoNarrativeReviewContract.validate(
                    media(69), json.createObjectNode(), true, evidence(69)))
        .hasMessageContaining("contrato de narrativa");
    var review = review(json).put("mode", "DECORATIVE");
    assertThatThrownBy(
            () -> VideoNarrativeReviewContract.validate(media(69), review, true, evidence(69)))
        .hasMessageContaining("decorativa");
    assertThatCode(
            () -> VideoNarrativeReviewContract.validate(media(69), review, false, evidence(69)))
        .doesNotThrowAnyException();
  }

  /** Cada falha editorial pode ser descrita sem ser compensada por outros critérios. */
  @ParameterizedTest
  @ValueSource(strings = {"expectation", "causalProgression", "payoff", "productBridge", "pacing"})
  void refusesDeficitEvenWhenOtherScoresAreHigh(String criterion) {
    var review = review(json);
    ((ObjectNode) review.path(criterion)).put("score", 2);
    assertThatThrownBy(
            () -> VideoNarrativeReviewContract.validate(media(69), review, true, evidence(69)))
        .hasMessageContaining(criterion);
    assertThatCode(
            () -> VideoNarrativeReviewContract.validate(media(69), review, false, evidence(69)))
        .doesNotThrowAnyException();
  }

  /**
   * Não permite citar outro vídeo, outro hash ou frame não entregue, mesmo em parecer de ajuste.
   */
  @Test
  void rejectsForeignAndInventedEvidence() {
    assertThatThrownBy(
            () ->
                VideoNarrativeReviewContract.validate(media(69), review(json), false, evidence(70)))
        .hasMessageContaining("outra mídia");
    assertThatThrownBy(
            () ->
                VideoNarrativeReviewContract.validate(
                    media(69).put("sha256", "b".repeat(64)), review(json), false, evidence(69)))
        .hasMessageContaining("outra mídia");
    assertThatThrownBy(
            () -> VideoNarrativeReviewContract.validate(media(69), review(json), false, List.of()))
        .hasMessageContaining("não recebido");
  }

  /** A sequência precisa avançar no tempo real do arquivo, não apenas declarar fases corretas. */
  @Test
  void rejectsRepeatedReversedAndOutOfRangeFrames() {
    var review = review(json);
    ((ObjectNode) review.path("moments").get(1)).put("evidenceId", 1001);
    assertThatThrownBy(
            () -> VideoNarrativeReviewContract.validate(media(69), review, true, evidence(69)))
        .hasMessageContaining("repetido");
    var reversed = review(json);
    ((ObjectNode) reversed.path("moments").get(0)).put("evidenceId", 1003);
    ((ObjectNode) reversed.path("moments").get(2)).put("evidenceId", 1001);
    assertThatThrownBy(
            () -> VideoNarrativeReviewContract.validate(media(69), reversed, false, evidence(69)))
        .hasMessageContaining("ordem temporal");
    assertThatThrownBy(
            () ->
                VideoNarrativeReviewContract.validate(
                    media(69).put("durationSeconds", 10), review(json), true, evidence(69)))
        .hasMessageContaining("duração");
  }

  /** Ausência de prova permite relatar o bloqueio sem fabricar momentos nem aprovar. */
  @Test
  void permitsUnverifiableWithoutInventingFrames() {
    var review = review(json).put("mode", "UNVERIFIABLE");
    review.putArray("moments");
    assertThatCode(
            () ->
                VideoNarrativeReviewContract.validate(
                    json.createObjectNode(), review, false, List.of()))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () -> VideoNarrativeReviewContract.validate(media(69), review, true, evidence(69)))
        .hasMessageContaining("não verificável");
  }

  /** Usa a mesma mídia sintética com duração conhecida, sem resultado de cliente. */
  private ObjectNode media(long assetId) {
    return json.createObjectNode()
        .put("assetId", assetId)
        .put("sha256", "a".repeat(64))
        .put("durationSeconds", 20);
  }

  /** Produz um parecer de teste; as notas não representam avaliação de agentes reais. */
  static ObjectNode review(ObjectMapper json) {
    var value =
        json.createObjectNode()
            .put("contractVersion", "VIDEO_NARRATIVE_REVIEW_V1")
            .put("mode", "PROGRESSION");
    for (String key :
        List.of("expectation", "causalProgression", "payoff", "productBridge", "pacing"))
      value
          .putObject(key)
          .put("score", 4)
          .put(
              "assessment",
              "Fixture sintética: situação e ação levam a uma prova legível do kit; não mede retenção.");
    var moments = value.putArray("moments");
    String[] roles = {"OPENING", "DEVELOPMENT", "PAYOFF"};
    String[] actions = {
      "A profissional tenta escrever o post.",
      "Preenche o serviço no formulário.",
      "Compara a arte e a legenda do kit."
    };
    String[] information = {
      "Falta a mensagem para acompanhar o trabalho.",
      "Uma entrada orienta a preparação do kit.",
      "A amostra resolve a dificuldade apresentada."
    };
    for (int i = 0; i < 3; i++)
      moments
          .addObject()
          .put("role", roles[i])
          .put("evidenceId", 1001 + i)
          .put("visibleAction", actions[i])
          .put("newInformation", information[i]);
    return value;
  }

  /** Cria frames recebidos da mesma mídia, com instantes e IDs independentes do produto. */
  static List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> evidence(long assetId) {
    return java.util.stream.IntStream.range(0, 3)
        .mapToObj(
            i ->
                new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
                    1001L + i,
                    "test-session",
                    "video-"
                        + assetId
                        + "-"
                        + "a".repeat(64)
                        + "-"
                        + new int[] {100, 5000, 15000}[i],
                    "VIDEO_FRAME",
                    "Fixture",
                    "IPHONE_15_PRO",
                    1,
                    null,
                    393,
                    700,
                    700,
                    0,
                    "https://fixtures.invalid/video.mp4",
                    "https://fixtures.invalid/video.mp4",
                    "/fixture.png",
                    1L,
                    "f".repeat(64),
                    Instant.parse("2026-10-15T00:00:00Z"),
                    "/tmp/fixture.png"))
        .toList();
  }
}
