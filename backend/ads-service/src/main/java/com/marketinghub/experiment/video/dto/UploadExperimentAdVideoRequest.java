package com.marketinghub.experiment.video.dto;

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
    boolean requiredForRelease) {}
