package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir aceite funcional que ignore a entrada ou tolere arte genérica. */
class KitPremiumVisualReviewTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Preserva os pixels do modal sem permitir trocar uma página inteira por recorte parcial. */
  @Test
  void restrictsViewportToExpandedArtwork() {
    var artifact = json.createObjectNode().put("evidenceType", "FOLD").put("foldNumber", 1);
    for (String suffix : List.of("-ART", "-STORY")) {
      artifact.put("evidenceKey", "ADHERENT-DESKTOP_1440" + suffix);
      assertThat(KitPremiumVisualReview.isArtworkViewport(artifact)).isTrue();
    }
    for (String suffix : List.of("", "-BRIEFING", "-CONTENT")) {
      artifact.put("evidenceKey", "ADHERENT-DESKTOP_1440" + suffix);
      assertThat(KitPremiumVisualReview.isArtworkViewport(artifact)).isFalse();
    }
    artifact.put("evidenceKey", "RECOVERY-IPHONE_15_PRO-STORY").put("foldNumber", 0);
    assertThat(KitPremiumVisualReview.isArtworkViewport(artifact)).isFalse();
  }

  /** Comprova cobertura sem depender do produto original, mantendo o contrato de outros PDEs. */
  @Test
  void requiresRealCoverageAcrossIdentities() {
    for (long id : new long[] {7, 9018}) {
      var target = json.createObjectNode().put("productId", id);
      target
          .putObject("pdeContext")
          .putObject("privatePrototypeAcceptance")
          .put("runtimeKind", "DETERMINISTIC_PRIVATE_KIT_V1");
      var report = report("ADHERENT");
      var images = images("ADHERENT");
      assertThatCode(() -> KitPremiumVisualReview.validateCapture(target, report, images))
          .doesNotThrowAnyException();
      assertThatThrownBy(
              () -> KitPremiumVisualReview.validateCapture(target, report, images.subList(1, 4)))
          .hasMessageContaining("pixels");
      ((ObjectNode) report).remove("visualReviewContract");
      assertThatThrownBy(() -> KitPremiumVisualReview.validateCapture(target, report, images))
          .hasMessageContaining("cobertura");
    }
    assertThatCode(
            () ->
                KitPremiumVisualReview.validateCapture(
                    json.createObjectNode(), json.createObjectNode(), List.of()))
        .doesNotThrowAnyException();
  }

  /** Aceita qualidade forte e a tela segura sem inventar necessidade de uma arte bloqueada. */
  @Test
  void acceptsStrongFinishAndSafetyWithoutArtwork() {
    for (String scenario : List.of("ADHERENT", "RECOVERY", "SAFETY"))
      assertThatCode(
              () ->
                  KitPremiumVisualReview.validateResult(
                      report(scenario), result(scenario), images(scenario)))
          .doesNotThrowAnyException();
  }

  /** Recusa aprovar uma nota funcional, ausente, textual ou fora da escala em qualquer dimensão. */
  @Test
  void rejectsFunctionalOnlyOrInvalidScores() {
    for (String field :
        List.of("professionalFinish", "specialtyRelevance", "legibility", "perceivedValue")) {
      var result = result("ADHERENT");
      var value = (ObjectNode) result.path("visualQuality").path("assessments").get(0).path(field);
      for (int score : new int[] {-1, 0, 1, 2, 3, 6}) {
        value.put("score", score);
        assertThatThrownBy(() -> validate(result)).isInstanceOf(IllegalArgumentException.class);
      }
      value.put("score", "5");
      assertThatThrownBy(() -> validate(result)).isInstanceOf(IllegalArgumentException.class);
      value.remove("score");
      assertThatThrownBy(() -> validate(result)).isInstanceOf(IllegalArgumentException.class);
    }
  }

  /** Recusa aprovação contraditória e preserva o ajuste com causa e ação explícitas. */
  @Test
  void rejectsPlaceholderAndCriticalDeficitButPreservesAdjustment() {
    for (String field : List.of("placeholderOnly", "criticalDeficitPresent")) {
      var result = result("ADHERENT");
      ((ObjectNode) result.path("visualQuality").path("assessments").get(0)).put(field, true);
      assertThatThrownBy(() -> validate(result)).hasMessageContaining("placeholder");
      result.put("decision", "ADJUST");
      assertThatThrownBy(() -> validate(result)).hasMessageContaining("correção");
      result
          .putArray("requiredChanges")
          .add("Substituir o cartão genérico por peça pertinente e reavaliar os pixels.");
      assertThatCode(() -> validate(result)).doesNotThrowAnyException();
    }
  }

  /** Impede omissão, duplicidade, troca de superfície e uso de imagens de outra execução. */
  @Test
  void rejectsUnboundVisualAssessments() {
    for (String field : List.of("evidenceId", "surfaceRole", "criticalDeficit")) {
      var result = result("ADHERENT");
      ((ObjectNode) result.path("visualQuality").path("assessments").get(0)).remove(field);
      assertThatThrownBy(() -> validate(result)).isInstanceOf(IllegalArgumentException.class);
    }
    var missing = result("ADHERENT");
    ((com.fasterxml.jackson.databind.node.ArrayNode)
            missing.path("visualQuality").path("assessments"))
        .remove(0);
    assertThatThrownBy(() -> validate(missing)).hasMessageContaining("todas");
    var absent = result("ADHERENT");
    absent.remove("visualQuality");
    assertThatThrownBy(() -> validate(absent)).hasMessageContaining("estruturada");
    assertThatCode(
            () ->
                KitPremiumVisualReview.validateResult(
                    json.createObjectNode(), absent, images("ADHERENT")))
        .doesNotThrowAnyException();
  }

  /** Executa o contrato com prova determinística fixa, sem invocar modelo. */
  private void validate(ObjectNode result) {
    KitPremiumVisualReview.validateResult(report("ADHERENT"), result, images("ADHERENT"));
  }

  /** Cria a prova de cobertura sintética equivalente ao resultado do navegador. */
  private ObjectNode report(String scenario) {
    var report =
        json.createObjectNode().put("visualReviewContract", KitPremiumVisualReview.VERSION);
    var keys =
        report
            .putArray("scenarios")
            .addObject()
            .put("scenarioCode", scenario)
            .put("deviceProfile", "DESKTOP_1440")
            .putArray("screenshotEvidenceKeys");
    images(scenario).forEach(image -> keys.add(image.evidenceKey()));
    return report;
  }

  /** Cria recibos de imagens isolados para testar vínculos sem artefatos produtivos. */
  private List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> images(String scenario) {
    var images = new ArrayList<BpmVisualEvidenceBackendClient.UploadedVisualEvidence>();
    for (String suffix :
        scenario.equals("SAFETY") ? List.of("") : List.of("-BRIEFING", "-ART", "-STORY", "")) {
      long id = 900L + images.size();
      images.add(
          new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
              id,
              "test-session",
              scenario + "-DESKTOP_1440" + suffix,
              "FULL_PAGE",
              "Fixture",
              "DESKTOP_1440",
              1,
              null,
              1440,
              900,
              900,
              0,
              "https://fixture.invalid",
              "https://fixture.invalid",
              "/fixture/" + id,
              300L,
              "a".repeat(64),
              java.time.Instant.parse("2026-10-30T00:00:00Z"),
              "/fixture.png"));
    }
    return images;
  }

  /** Simula interpretação com notas fortes, sem apresentá-la como parecer real de Psique. */
  private ObjectNode result(String scenario) {
    var result = json.createObjectNode().put("decision", "APPROVED");
    result.putArray("requiredChanges");
    var assessments =
        result
            .putObject("visualQuality")
            .put("contractVersion", KitPremiumVisualReview.VERSION)
            .putArray("assessments");
    for (var image : images(scenario)) {
      var row =
          assessments
              .addObject()
              .put("evidenceId", image.id())
              .put("surfaceRole", KitPremiumVisualReview.role(image.evidenceKey()))
              .put("placeholderOnly", false)
              .put("criticalDeficitPresent", false)
              .put("criticalDeficit", "Nenhum déficit na fixture simulada.");
      for (String name :
          List.of("professionalFinish", "specialtyRelevance", "legibility", "perceivedValue"))
        row.putObject(name)
            .put("score", scenario.equals("SAFETY") ? 3 : 4)
            .put("evidence", "Evidência sintética isolada para teste de contrato.");
    }
    return result;
  }
}
