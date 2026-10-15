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
    assertThatCode(() -> validate(task, result)).doesNotThrowAnyException();
    ((ObjectNode) result.path("videoAudit").get(0)).put("sha256", "b".repeat(64));
    assertThatThrownBy(() -> validate(task, result)).hasMessageContaining("identidade");
  }

  /** Os déficits apontados pelo usuário impedem aprovação, mesmo com vídeo reproduzível. */
  @ParameterizedTest
  @ValueSource(
      strings = {"clarity", "desire", "firstAction", "proofLegibility", "visualProgression"})
  void refusesLowQualityApproval(String criterion) {
    var result = result(53);
    ((ObjectNode) result.path("videoAudit").get(0)).put(criterion, 1);
    assertThatThrownBy(() -> validate(task(53), result)).hasMessageContaining(criterion);
    result.put("decision", "ADJUST");
    assertThatCode(() -> validate(task(53), result)).doesNotThrowAnyException();
  }

  /** Não aceita um relatório do PNG como se tivesse analisado vídeo nem audição inexistente. */
  @Test
  void refusesAbsentVideoAuditAndFalseListening() {
    assertThatThrownBy(() -> validate(task(53), json.createObjectNode()))
        .hasMessageContaining("cada vídeo");
    var result = result(53);
    ((ObjectNode) result.path("videoAudit").get(0)).put("audioAssessment", "LISTENED");
    assertThatThrownBy(() -> validate(task(53), result)).hasMessageContaining("áudio");
    assertThatCode(() -> validate(Map.of(), json.createObjectNode())).doesNotThrowAnyException();
  }

  /** O retorno exclusivo de vídeo requer inspeção e não pode coexistir com uma aprovação. */
  @Test
  void preservesCorrectionScopeWithoutInventingReview() {
    var result = result(53);
    result.put("correctionScope", "FINAL_VIDEO");
    assertThatThrownBy(() -> validate(task(53), result)).hasMessageContaining("escopo");
    result.put("decision", "ADJUST");
    assertThatCode(() -> validate(task(53), result)).doesNotThrowAnyException();
    assertThatThrownBy(() -> validate(Map.of(), result)).hasMessageContaining("inspecionados");
  }

  /** Reproduz o aceite apenas visual de Capella: novas revisões não podem omitir a copy. */
  @Test
  void refusesVisualOnlyReviewEvenWithHighScores() {
    var result = result(69);
    var item = (ObjectNode) result.path("videoAudit").get(0);
    item.remove("copyReview");
    assertThatThrownBy(() -> validate(task(69), result)).hasMessageContaining("contrato de copy");
    item.set("copyReview", VideoCopyReviewContractTest.review(json).put("mode", "DESCRIPTIVE"));
    assertThatThrownBy(() -> validate(task(69), result)).hasMessageContaining("descritiva");
    result.put("decision", "ADJUST");
    assertThatCode(() -> validate(task(69), result)).doesNotThrowAnyException();
  }

  /** Valida a entrada com os frames persistidos que a mesma tentativa teria recebido. */
  private void validate(Map<String, Object> task, com.fasterxml.jackson.databind.JsonNode result) {
    long id = FinalVideoReviewInputs.inputs(json, task).path(0).path("assetId").asLong();
    FinalVideoReviewInputs.validate(
        json, task, result, VideoNarrativeReviewContractTest.evidence(id));
  }

  /** Uma nota alta de progressão sem os momentos comprovados não basta para aprovar. */
  @Test
  void refusesNarrativeWithoutTemporalEvidence() {
    var result = result(69);
    ((ObjectNode) result.path("videoAudit").get(0)).remove("narrativeReview");
    assertThatThrownBy(() -> validate(task(69), result))
        .hasMessageContaining("contrato de narrativa");
  }

  /** Não compensa incompreensão ou sobrecarga com beleza, copy ou narrativa bem pontuadas. */
  @ParameterizedTest
  @ValueSource(strings = {"processingFluency", "sensoryCongruence", "overloadRisk"})
  void refusesContradictorySensoryApprovalAcrossProducts(String criterion) throws Exception {
    for (long id : new long[] {69, 910069}) {
      var result = result(id);
      var sensory = (ObjectNode) result.at("/behavioralResponse/sensoryExperience");
      sensory.put(criterion, "overloadRisk".equals(criterion) ? 3 : 2);
      assertThatThrownBy(() -> validate(task(id), result)).hasMessageContaining(criterion);
      result.put("decision", "ADJUST");
      assertThatCode(() -> validate(task(id), result)).doesNotThrowAnyException();
      var replay = json.readTree(result.toString());
      assertThatCode(() -> validate(task(id), replay)).doesNotThrowAnyException();
      result.put("decision", "BLOCKED");
      assertThatCode(() -> validate(task(id), result)).doesNotThrowAnyException();
    }
  }

  /** Preserva o patamar sensorial histórico aceito, sem impor prazer máximo ou exigir pessoas. */
  @Test
  void preservesHistoricalSensoryBoundaryAndStaticReviews() {
    var result = result(69);
    var sensory = (ObjectNode) result.at("/behavioralResponse/sensoryExperience");
    sensory.put("processingFluency", 3).put("sensoryCongruence", 3).put("overloadRisk", 2);
    ((ObjectNode) sensory.path("pleasureByModality").get(0)).put("pleasureScore", 3);
    assertThatCode(() -> validate(task(69), result)).doesNotThrowAnyException();
    sensory.put("overloadRisk", 5);
    assertThatCode(() -> validate(Map.of(), result)).doesNotThrowAnyException();
  }

  /** Aprovação exige modalidade visual comprovada, sem converter ausência em nota favorável. */
  @Test
  void refusesSensoryApprovalWithoutEvidence() {
    var result = result(69);
    var sensory = (ObjectNode) result.at("/behavioralResponse/sensoryExperience");
    sensory.put("evidenceAvailable", false);
    sensory.putArray("availableModalities");
    sensory.putArray("pleasureByModality");
    sensory.put("processingFluency", 0).put("sensoryCongruence", 0).put("overloadRisk", 0);
    assertThatThrownBy(() -> validate(task(69), result)).hasMessageContaining("visual");
    result.put("decision", "BLOCKED");
    assertThatCode(() -> validate(task(69), result)).doesNotThrowAnyException();
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
                        "durationSeconds",
                        20,
                        "narration",
                        VideoCopyReviewContractTest.NARRATION)))));
  }

  /** Produz um parecer simulado exclusivamente para testar a validação determinística. */
  private ObjectNode result(long id) {
    var result = json.createObjectNode().put("decision", "APPROVED");
    var sensory = result.putObject("behavioralResponse").putObject("sensoryExperience");
    sensory.put("evidenceAvailable", true);
    sensory.putArray("availableModalities").add("VISUAL");
    sensory
        .putArray("pleasureByModality")
        .addObject()
        .put("modality", "VISUAL")
        .put("pleasureScore", 4)
        .put("evidence", "Fixture sintética: prova com hierarquia legível.");
    sensory
        .put("processingFluency", 3)
        .put("sensoryCongruence", 3)
        .put("overloadRisk", 2)
        .put("embodiedAnticipation", "Hipótese de consultar o kit, sem cliente real.")
        .put("dominantCue", "Prova central no frame da recompensa.")
        .put("evidenceBoundary", "Frames e texto, sem audição ou continuidade comprovada.");
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
    audit.set("narrativeReview", VideoNarrativeReviewContractTest.review(json));
    return result;
  }
}
