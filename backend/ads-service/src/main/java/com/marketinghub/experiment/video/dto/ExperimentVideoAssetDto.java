package com.marketinghub.experiment.video.dto;

import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Representação de leitura do vídeo vinculado ao experimento.
 */
public record ExperimentVideoAssetDto(
        Long id,
        Long experimentId,
        ExperimentVideoSlot slot,
        String objective,
        String primaryMetric,
        String script,
        String prompt,
        String provider,
        String model,
        ExperimentVideoStatus status,
        String assetUrl,
        String thumbnailUrl,
        Integer durationSeconds,
        String aspectRatio,
        String requestJson,
        String responseJson,
        BigDecimal cost,
        ExperimentVideoReviewStatus reviewStatus,
        boolean requiredForRelease,
        Long salesVideoProfileId,
        Long salesVideoJobId,
        Long assetId,
        Long landingVideoSlotId,
        Instant createdAt,
        Instant updatedAt
) { }
