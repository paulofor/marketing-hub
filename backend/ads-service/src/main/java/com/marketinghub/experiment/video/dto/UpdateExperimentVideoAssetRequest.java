package com.marketinghub.experiment.video.dto;

import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import java.math.BigDecimal;

/**
 * Dados opcionais para atualizar estado, revisão e vínculos do vídeo do experimento.
 */
public record UpdateExperimentVideoAssetRequest(
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
        String rejectionReason,
        String reviewedBy,
        Boolean requiredForRelease,
        Long salesVideoProfileId,
        Long salesVideoJobId,
        Long assetId,
        Long landingVideoSlotId
) { }
