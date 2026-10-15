package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.List;

/** Vincula o parecer narrativo aos frames recebidos, sem confundir montagem com progressão. */
final class VideoNarrativeReviewContract {
  private static final List<String> CRITERIA =
      List.of("expectation", "causalProgression", "payoff", "productBridge", "pacing");
  private static final List<String> MOMENTS = List.of("OPENING", "DEVELOPMENT", "PAYOFF");

  /** Impede instanciar o contrato sem estado. */
  private VideoNarrativeReviewContract() {}

  /** Confere diagnóstico, critérios e três momentos temporais da mesma mídia antes do aceite. */
  static void validate(
      JsonNode media,
      JsonNode review,
      boolean approved,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> evidence) {
    String mode = review.path("mode").asText();
    if (!"VIDEO_NARRATIVE_REVIEW_V1".equals(review.path("contractVersion").asText())
        || !List.of("PROGRESSION", "DECORATIVE", "UNVERIFIABLE").contains(mode))
      throw new IllegalArgumentException("Parecer sem contrato de narrativa audiovisual.");
    if (approved && !"PROGRESSION".equals(mode))
      throw new IllegalArgumentException(
          "Narrativa decorativa ou não verificável impede aprovação.");
    for (String criterion : CRITERIA) {
      JsonNode value = review.path(criterion);
      JsonNode score = value.path("score");
      if (!score.isIntegralNumber()
          || score.asInt() < 0
          || score.asInt() > 5
          || !text(value, "assessment"))
        throw new IllegalArgumentException("Critério narrativo incompleto: " + criterion);
      if (approved && score.asInt() < 3)
        throw new IllegalArgumentException("Critério narrativo insuficiente: " + criterion);
    }
    JsonNode beats = review.path("moments");
    if (!beats.isArray()
        || (beats.size() != 3 && !("UNVERIFIABLE".equals(mode) && beats.isEmpty())))
      throw new IllegalArgumentException("Narrativa exige abertura, desenvolvimento e recompensa.");
    if (beats.isEmpty()) return;
    double duration = media.path("durationSeconds").asDouble(-1);
    if (!media.path("durationSeconds").isNumber() || !Double.isFinite(duration) || duration <= 0)
      throw new IllegalArgumentException("Duração do vídeo ausente para auditar narrativa.");
    String prefix =
        "video-" + media.path("assetId").asLong() + "-" + media.path("sha256").asText() + "-";
    double previous = -1;
    var seen = new HashSet<Long>();
    for (int index = 0; index < beats.size(); index++) {
      JsonNode beat = beats.get(index);
      JsonNode id = beat.path("evidenceId");
      if (!id.isIntegralNumber()
          || !id.canConvertToLong()
          || id.asLong() <= 0
          || !seen.add(id.asLong())
          || !MOMENTS.get(index).equals(beat.path("role").asText())
          || !text(beat, "visibleAction")
          || !text(beat, "newInformation"))
        throw new IllegalArgumentException(
            "Momento narrativo incompleto, repetido ou fora de ordem.");
      var frame =
          (evidence == null
                  ? List.<BpmVisualEvidenceBackendClient.UploadedVisualEvidence>of()
                  : evidence)
              .stream()
                  .filter(item -> Long.valueOf(id.asLong()).equals(item.id()))
                  .findFirst()
                  .orElseThrow(
                      () ->
                          new IllegalArgumentException(
                              "Frame narrativo não recebido nesta revisão."));
      String key = frame.evidenceKey();
      if (!"VIDEO_FRAME".equals(frame.evidenceType())
          || key == null
          || !key.startsWith(prefix)
          || !key.substring(prefix.length()).matches("[0-9]{1,9}"))
        throw new IllegalArgumentException("Frame narrativo pertence a outra mídia ou versão.");
      double second = Long.parseLong(key.substring(prefix.length())) / 1000.0;
      if (second <= previous || second >= duration)
        throw new IllegalArgumentException("Frame narrativo fora da duração ou ordem temporal.");
      previous = second;
    }
  }

  /** Exige descrição concreta preenchida sem converter tipos alheios em texto aparente. */
  private static boolean text(JsonNode value, String field) {
    return value.path(field).isTextual() && !value.path(field).asText().isBlank();
  }
}
