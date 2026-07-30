package com.marketinghub.pde.service.versionvideos;

import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;

/** Representa um vídeo HLS resolvido para uma versão produtiva do PDE. */
public record PdeProductionSlotVideoAssetDto(
    Long id,
    Long experimentId,
    String assignmentSource,
    String objective,
    String primaryMetric,
    String provider,
    String model,
    ExperimentVideoStatus status,
    ExperimentVideoReviewStatus reviewStatus,
    String assetUrl,
    String hlsPlaybackUrl,
    String thumbnailUrl,
    Integer durationSeconds,
    Long salesVideoProfileId,
    Long salesVideoJobId,
    Long assetId,
    Long landingVideoSlotId) {}
