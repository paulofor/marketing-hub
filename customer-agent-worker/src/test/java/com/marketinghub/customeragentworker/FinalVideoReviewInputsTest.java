package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Evita aprovação de vídeos por parecer estático, versão errada ou modalidade não inspecionada. */
class FinalVideoReviewInputsTest {
  private final ObjectMapper json = new ObjectMapper();

  /**
   * Mira e outro produto usam a mesma vinculação ao arquivo final, sem condições por identidade.
   */
  @ParameterizedTest
  @ValueSource(longs = {53, 910305})
  void acceptsOnlyTheExactReviewedVideo(long id) {
    var task = task(id);
    var result = result(id);
    assertThatCode(() -> FinalVideoReviewInputs.validate(json, task, result))
        .doesNotThrowAnyException();
    ((ObjectNode) result.path("videoAudit").get(0)).put("sha256", "b".repeat(64));
    assertThatThrownBy(() -> FinalVideoReviewInputs.validate(json, task, result))
        .hasMessageContaining("identidade");
  }

  /** Os déficits apontados pelo usuário impedem aprovação, mesmo com vídeo reproduzível. */
  @ParameterizedTest
  @ValueSource(
      strings = {"clarity", "desire", "firstAction", "proofLegibility", "visualProgression"})
  void refusesLowQualityApproval(String criterion) {
    var result = result(53);
    ((ObjectNode) result.path("videoAudit").get(0)).put(criterion, 1);
    assertThatThrownBy(() -> FinalVideoReviewInputs.validate(json, task(53), result))
        .hasMessageContaining(criterion);
    result.put("decision", "ADJUST");
    assertThatCode(() -> FinalVideoReviewInputs.validate(json, task(53), result))
        .doesNotThrowAnyException();
  }

  /** Não aceita um relatório do PNG como se tivesse analisado vídeo nem audição inexistente. */
  @Test
  void refusesAbsentVideoAuditAndFalseListening() {
    assertThatThrownBy(
            () -> FinalVideoReviewInputs.validate(json, task(53), json.createObjectNode()))
        .hasMessageContaining("cada vídeo");
    var result = result(53);
    ((ObjectNode) result.path("videoAudit").get(0)).put("audioAssessment", "LISTENED");
    assertThatThrownBy(() -> FinalVideoReviewInputs.validate(json, task(53), result))
        .hasMessageContaining("áudio");
    assertThatCode(() -> FinalVideoReviewInputs.validate(json, Map.of(), json.createObjectNode()))
        .doesNotThrowAnyException();
  }

  /** O retorno exclusivo de vídeo requer inspeção e não pode coexistir com uma aprovação. */
  @Test
  void preservesCorrectionScopeWithoutInventingReview() {
    var result = result(53);
    result.put("correctionScope", "FINAL_VIDEO");
    assertThatThrownBy(() -> FinalVideoReviewInputs.validate(json, task(53), result))
        .hasMessageContaining("escopo");
    result.put("decision", "ADJUST");
    assertThatCode(() -> FinalVideoReviewInputs.validate(json, task(53), result))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> FinalVideoReviewInputs.validate(json, Map.of(), result))
        .hasMessageContaining("inspecionados");
  }

  /** Reproduz o aceite apenas visual de Capella: novas revisões não podem omitir a copy. */
  @Test
  void refusesVisualOnlyReviewEvenWithHighScores() {
    var result = result(69);
    var item = (ObjectNode) result.path("videoAudit").get(0);
    item.remove("copyReview");
    assertThatThrownBy(() -> FinalVideoReviewInputs.validate(json, task(69), result))
        .hasMessageContaining("contrato de copy");
    item.set("copyReview", VideoCopyReviewContractTest.review(json).put("mode", "DESCRIPTIVE"));
    assertThatThrownBy(() -> FinalVideoReviewInputs.validate(json, task(69), result))
        .hasMessageContaining("descritiva");
    result.put("decision", "ADJUST");
    assertThatCode(() -> FinalVideoReviewInputs.validate(json, task(69), result))
        .doesNotThrowAnyException();
  }

  /** Monta uma entrada isolada de inspeção, sem autorização de mídia ou prova de mercado. */
  private Map<String, Object> task(long id) {
    return Map.of(
        "taskTarget",
        Map.of(
            "pdeContext",
            Map.of(
                "finalVideoReview",
                java.util.List.of(
                    Map.of(
                        "assetId",
                        id,
                        "sha256",
                        "a".repeat(64),
                        "narration",
                        VideoCopyReviewContractTest.NARRATION)))));
  }

  /** Produz um parecer simulado exclusivamente para testar a validação determinística. */
  private ObjectNode result(long id) {
    var result = json.createObjectNode().put("decision", "APPROVED");
    var audit =
        result
            .putArray("videoAudit")
            .addObject()
            .put("assetId", id)
            .put("sha256", "a".repeat(64))
            .put("criticalDeficit", false)
            .put("audioAssessment", "NOT_LISTENED")
            .put(
                "evidence",
                "0,1 s: contexto; 2 s: ação; 8 s: resultado. Fixture, não comportamento de mercado.");
    for (String key :
        java.util.List.of(
            "clarity", "desire", "firstAction", "proofLegibility", "visualProgression"))
      audit.put(key, 4);
    audit.set("copyReview", VideoCopyReviewContractTest.review(json));
    return result;
  }
}
