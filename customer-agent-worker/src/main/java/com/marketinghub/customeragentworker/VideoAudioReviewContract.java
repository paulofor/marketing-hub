package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/** Impede audição fictícia e exige correspondência temporal entre som e frames da mesma mídia. */
final class VideoAudioReviewContract {
  /** Impede instanciação do contrato sem estado. */
  private VideoAudioReviewContract() {}

  /**
   * Confere recibo e pares momento/frame; um trecho pode sustentar mais de uma observação distinta.
   */
  static void validate(
      JsonNode task,
      JsonNode media,
      JsonNode item,
      boolean approved,
      List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> evidence) {
    JsonNode receipt = null;
    for (var value : task.path("audioReviews"))
      if (value.path("assetId").asLong() == media.path("assetId").asLong()) receipt = value;
    boolean required = task.at("/taskTarget/pdeContext/audioReviewRequired").asBoolean();
    if (receipt == null) {
      if (!"NOT_LISTENED".equals(item.path("audioAssessment").asText()) || (required && approved))
        throw new IllegalArgumentException("Parecer sem evidência de áudio ouvido.");
      return;
    }
    if (!"COMPLETED".equals(receipt.path("status").asText())
        || !media.path("sha256").asText().equals(receipt.path("videoSha256").asText())
        || item.path("audioReviewId").asLong() != receipt.path("id").asLong()
        || !"LISTENED".equals(item.path("audioAssessment").asText()))
      throw new IllegalArgumentException("Recibo de áudio divergente da mídia.");
    var assessment = receipt.path("assessment");
    if (approved && !"PASS".equals(assessment.path("verdict").asText()))
      throw new IllegalArgumentException("Reprovação ou dúvida auditiva impede aprovação.");
    var sync = item.path("audioVisualSync");
    if (!sync.isArray() || sync.size() < 3)
      throw new IllegalArgumentException("Sincronização sem abertura, meio e encerramento.");
    var seen = new HashSet<String>();
    boolean beginning = false, middle = false, end = false;
    double duration = media.path("durationSeconds").asDouble();
    for (var relation : sync) {
      int index = relation.path("audioMomentIndex").asInt(-1);
      var moment = assessment.path("moments").path(index);
      if (index < 0
          || moment.isMissingNode()
          || !seen.add(index + ":" + relation.path("frameEvidenceId").asLong())
          || relation.path("reason").asText().length() < 15)
        throw new IllegalArgumentException("Momento auditivo ausente ou repetido.");
      if (approved && !"PASS".equals(relation.path("status").asText()))
        throw new IllegalArgumentException("Sincronização insuficiente impede aprovação.");
      var frame =
          evidence.stream()
              .filter(e -> e.id() == relation.path("frameEvidenceId").asLong())
              .findFirst()
              .orElseThrow(
                  () -> new IllegalArgumentException("Frame de sincronização inexistente."));
      String prefix =
          "video-" + media.path("assetId").asLong() + "-" + media.path("sha256").asText() + "-";
      if (!"VIDEO_FRAME".equals(frame.evidenceType()) || !frame.evidenceKey().startsWith(prefix))
        throw new IllegalArgumentException("Frame pertence a outra mídia.");
      double seconds = Long.parseLong(frame.evidenceKey().substring(prefix.length())) / 1000.0;
      if (seconds < moment.path("startSeconds").asDouble() - 1
          || seconds > moment.path("endSeconds").asDouble() + 1)
        throw new IllegalArgumentException("Frame fora do momento ouvido.");
      beginning |= seconds <= duration * .25;
      middle |= seconds >= duration * .25 && seconds <= duration * .65;
      end |= seconds >= duration * .65;
    }
    if (!beginning || !middle || !end)
      throw new IllegalArgumentException("Sincronização não cobre abertura, meio e encerramento.");
  }
}
