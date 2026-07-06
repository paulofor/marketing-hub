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
        String aspectRatio,
        String requestJson,
        String responseJson,
        BigDecimal cost,
        ExperimentVideoReviewStatus reviewStatus,
        Boolean requiredForRelease,
        Long salesVideoProfileId,
        Long salesVideoJobId,
        Long assetId,
        Long landingVideoSlotId
) { }
