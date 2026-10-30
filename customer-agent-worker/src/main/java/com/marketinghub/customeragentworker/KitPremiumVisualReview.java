package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Responsabilidade: exigir cobertura e acabamento visual por captura nos pareceres novos de kits.
 */
final class KitPremiumVisualReview {
  static final String VERSION = "PDE_KIT_PREMIUM_VISUAL_V1";
  private static final List<String> DIMENSIONS =
      List.of("professionalFinish", "specialtyRelevance", "legibility", "perceivedValue");

  /** Impede instanciar um contrato sem estado. */
  private KitPremiumVisualReview() {}

  /**
   * Aceita viewport somente para a peça ampliada inteira, sem recortar a entrada ou o resultado.
   */
  static boolean isArtworkViewport(JsonNode artifact) {
    String key = artifact.path("evidenceKey").asText();
    return (key.endsWith("-ART") || key.endsWith("-STORY"))
        && "FOLD".equals(artifact.path("evidenceType").asText())
        && artifact.path("foldNumber").isIntegralNumber()
        && artifact.path("foldNumber").asInt() == 1;
  }

  /** Recusa cenários novos sem entrada, post, story e resultado da mesma sessão antes do modelo. */
  static void validateCapture(
      JsonNode target,
      JsonNode report,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> images) {
    if (!KitCustomerSurfaceProof.applies(target)) return;
    if (!VERSION.equals(report.path("visualReviewContract").asText())
        || report.path("scenarios").size() != 1)
      throw new IllegalArgumentException("Kit sem contrato de cobertura visual independente.");
    JsonNode scenario = report.path("scenarios").get(0);
    String code = scenario.path("scenarioCode").asText();
    String device = scenario.path("deviceProfile").asText();
    String prefix = code + "-" + device;
    Set<String> expected =
        "SAFETY".equals(code)
            ? Set.of(prefix)
            : Set.of(prefix, prefix + "-BRIEFING", prefix + "-ART", prefix + "-STORY");
    Set<String> actual = new HashSet<>();
    Set<Long> ids = new HashSet<>();
    Set<String> sessions = new HashSet<>();
    for (var image : images) {
      if (!actual.add(image.evidenceKey())
          || !ids.add(image.id())
          || !device.equals(image.deviceProfile()))
        throw new IllegalArgumentException("Kit com captura duplicada ou de outro dispositivo.");
      sessions.add(image.captureSessionId());
    }
    Set<String> linked = new HashSet<>();
    scenario.path("screenshotEvidenceKeys").forEach(key -> linked.add(key.asText()));
    if (!actual.equals(expected) || !linked.equals(expected) || sessions.size() != 1)
      throw new IllegalArgumentException(
          "Kit sem pixels vinculados da entrada, das artes e do resultado.");
  }

  /** Preserva replay histórico e impede aprovação nova com qualidade insuficiente ou sem pixels. */
  static void validateResult(
      JsonNode report,
      JsonNode result,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> images) {
    if (!VERSION.equals(report.path("visualReviewContract").asText())) return;
    JsonNode quality = result.path("visualQuality");
    if (!VERSION.equals(quality.path("contractVersion").asText())
        || !quality.path("assessments").isArray())
      throw new IllegalArgumentException("Kit sem avaliação estruturada do acabamento visual.");
    boolean approved = "APPROVED".equals(result.path("decision").asText());
    Set<Long> checked = new HashSet<>();
    for (JsonNode assessment : quality.path("assessments")) {
      long id = assessment.path("evidenceId").asLong(-1);
      var image =
          images.stream()
              .filter(value -> value.id() == id)
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Avaliação visual cita pixels de outra execução."));
      if (!checked.add(id)
          || !role(image.evidenceKey()).equals(assessment.path("surfaceRole").asText())
          || !assessment.path("placeholderOnly").isBoolean()
          || !assessment.path("criticalDeficitPresent").isBoolean()
          || assessment.path("criticalDeficit").asText().isBlank())
        throw new IllegalArgumentException("Avaliação visual incompleta ou duplicada.");
      for (String dimension : DIMENSIONS) {
        JsonNode score = assessment.path(dimension).path("score");
        int minimum = "SAFETY".equals(role(image.evidenceKey())) ? 3 : 4;
        if (!score.isIntegralNumber()
            || score.asInt() < 0
            || score.asInt() > 5
            || assessment.path(dimension).path("evidence").asText().isBlank())
          throw new IllegalArgumentException("Dimensão visual sem nota e evidência: " + dimension);
        if (approved && score.asInt() < minimum)
          throw new IllegalArgumentException("Kit aprovado sem acabamento premium: " + dimension);
      }
      if (approved
          && (assessment.path("placeholderOnly").asBoolean()
              || assessment.path("criticalDeficitPresent").asBoolean()))
        throw new IllegalArgumentException(
            "Kit aprovado com placeholder ou déficit visual crítico.");
    }
    if (checked.size() != images.size() || checked.isEmpty())
      throw new IllegalArgumentException("Avaliação visual não cobre todas as capturas do kit.");
    if (!approved && result.path("requiredChanges").isEmpty())
      throw new IllegalArgumentException(
          "Reprovação visual precisa indicar a correção ao construtor.");
  }

  /** Mapeia o momento capturado para a superfície, sem depender de nomes ou IDs de produtos. */
  static String role(String key) {
    if (key.startsWith("SAFETY-")) return "SAFETY";
    if (key.endsWith("-BRIEFING")) return "BRIEFING";
    if (key.endsWith("-ART")) return "POST";
    if (key.endsWith("-STORY")) return "STORY";
    return "RESULT";
  }
}
