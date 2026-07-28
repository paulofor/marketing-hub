package com.marketinghub.experiment.video.dto;

import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Dados para registrar um vídeo planejado ou gerado dentro de um experimento. */
public record CreateExperimentVideoAssetRequest(
    @NotNull ExperimentVideoSlot slot,
    @NotBlank String objective,
    @NotBlank String primaryMetric,
    String script,
    String prompt,
    @NotBlank String provider,
    @NotBlank String model,
    ExperimentVideoStatus status,
    String assetUrl,
    String thumbnailUrl,
    Integer durationSeconds,
    Boolean hasAudio,
    String aspectRatio,
    String visualSourceType,
    String visualSourceKey,
    String visualSourceDescription,
    String visualSimilarityOverrideReason,
    String requestJson,
    String responseJson,
    BigDecimal cost,
    BigDecimal audioCost,
    ExperimentVideoReviewStatus reviewStatus,
    boolean requiredForRelease,
    Long salesVideoProfileId,
    Long salesVideoJobId,
    Long assetId,
    Long landingVideoSlotId,
    String hlsPlaybackUrl) {}
