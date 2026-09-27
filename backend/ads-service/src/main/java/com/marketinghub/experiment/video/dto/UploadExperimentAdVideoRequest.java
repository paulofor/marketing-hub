package com.marketinghub.experiment.video.dto;

import java.util.List;

/**
 * Dados comerciais, HLS e de proveniência para vincular um vídeo vertical já finalizado a um
 * experimento.
 */
public record UploadExperimentAdVideoRequest(
    String objective,
    String primaryMetric,
    String script,
    Integer durationSeconds,
    Boolean hasAudio,
    String visualSourceKey,
    String visualSourceDescription,
    String productionReference,
    List<Long> visualSourceCreativeIds,
    List<Long> visualSourceVideoAssetIds,
    String hlsPlaybackUrl,
    boolean requiredForRelease) {

  /** Mantém compatibilidade com uploads que ainda não declaravam a entrega HLS. */
  public UploadExperimentAdVideoRequest(
      String objective,
      String primaryMetric,
      String script,
      Integer durationSeconds,
      Boolean hasAudio,
      String visualSourceKey,
      String visualSourceDescription,
      String productionReference,
      List<Long> visualSourceCreativeIds,
      List<Long> visualSourceVideoAssetIds,
      boolean requiredForRelease) {
    this(
        objective,
        primaryMetric,
        script,
        durationSeconds,
        hasAudio,
        visualSourceKey,
        visualSourceDescription,
        productionReference,
        visualSourceCreativeIds,
        visualSourceVideoAssetIds,
        null,
        requiredForRelease);
  }

  /** Mantém compatibilidade com uploads que declaravam somente criativos como fonte visual. */
  public UploadExperimentAdVideoRequest(
      String objective,
      String primaryMetric,
      String script,
      Integer durationSeconds,
      Boolean hasAudio,
      String visualSourceKey,
      String visualSourceDescription,
      String productionReference,
      List<Long> visualSourceCreativeIds,
      boolean requiredForRelease) {
    this(
        objective,
        primaryMetric,
        script,
        durationSeconds,
        hasAudio,
        visualSourceKey,
        visualSourceDescription,
        productionReference,
        visualSourceCreativeIds,
        List.of(),
        null,
        requiredForRelease);
  }
}
