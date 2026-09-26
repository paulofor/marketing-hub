package com.marketinghub.experiment.video.dto;

import java.util.List;

/**
 * Dados comerciais e de proveniência para vincular um vídeo vertical já finalizado a um
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
    boolean requiredForRelease) {}
