package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Verifica áudio ouvido, legendas atrasadas e isolamento dos frames na decisão audiovisual. */
class VideoAudioReviewContractTest {
  private final ObjectMapper json = new ObjectMapper();

  /**
   * Rejeita o defeito sonoro e a sincronia incorreta em dois produtos, preservando a versão boa.
   */
  @ParameterizedTest
  @ValueSource(longs = {69, 910069})
  void requiresSoundAndMatchingFrames(long id) {
    var task = json.createObjectNode();
    task.putObject("taskTarget").putObject("pdeContext").put("audioReviewRequired", true);
    var media =
        json.createObjectNode()
            .put("assetId", id)
            .put("sha256", "a".repeat(64))
            .put("durationSeconds", 20);
    var item =
        json.createObjectNode().put("audioAssessment", "LISTENED").put("audioReviewId", 5001);
    var receipt =
        task.putArray("audioReviews")
            .addObject()
            .put("id", 5001)
            .put("assetId", id)
            .put("videoSha256", "a".repeat(64))
            .put("status", "COMPLETED");
    var assessment = receipt.putObject("assessment").put("verdict", "PASS");
    var moments = assessment.putArray("moments");
    for (double start : new double[] {0, 5, 14})
      moments.addObject().put("startSeconds", start).put("endSeconds", start + 4);
    var sync = item.putArray("audioVisualSync");
    for (int i = 0; i < 3; i++)
      sync.addObject()
          .put("audioMomentIndex", i)
          .put("frameEvidenceId", 1001 + i)
          .put("status", "PASS")
          .put("reason", "Fala e demonstração correspondem neste instante simulado.");
    var frames = VideoNarrativeReviewContractTest.evidence(id);
    assertThatCode(() -> VideoAudioReviewContract.validate(task, media, item, true, frames))
        .doesNotThrowAnyException();
    assessment.put("verdict", "ADJUST");
    assertThatThrownBy(() -> VideoAudioReviewContract.validate(task, media, item, true, frames))
        .hasMessageContaining("auditiva");
    assertThatCode(() -> VideoAudioReviewContract.validate(task, media, item, false, frames))
        .doesNotThrowAnyException();
    assessment.put("verdict", "PASS");
    ((ObjectNode) sync.get(1)).put("status", "ADJUST");
    assertThatThrownBy(() -> VideoAudioReviewContract.validate(task, media, item, true, frames))
        .hasMessageContaining("Sincronização");
    ((ObjectNode) sync.get(1)).put("status", "PASS").put("frameEvidenceId", 1003);
    assertThatThrownBy(() -> VideoAudioReviewContract.validate(task, media, item, true, frames))
        .hasMessageContaining("momento ouvido");
    ((ObjectNode) sync.get(1)).put("frameEvidenceId", 1002);
    assertThatThrownBy(
            () ->
                VideoAudioReviewContract.validate(
                    task, media, item, true, VideoNarrativeReviewContractTest.evidence(id + 1)))
        .hasMessageContaining("outra mídia");
    media.put("durationSeconds", 100);
    assertThatThrownBy(() -> VideoAudioReviewContract.validate(task, media, item, true, frames))
        .hasMessageContaining("encerramento");
    media.put("durationSeconds", 20);
    task.remove("audioReviews");
    assertThatThrownBy(() -> VideoAudioReviewContract.validate(task, media, item, true, frames))
        .hasMessageContaining("áudio ouvido");
  }
}
