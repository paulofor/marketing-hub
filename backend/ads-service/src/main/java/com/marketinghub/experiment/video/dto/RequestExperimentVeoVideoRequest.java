package com.marketinghub.experiment.video.dto;

import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.salesvideo.SalesVideoExecutionMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitação administrativa para iniciar um vídeo VEO a partir de um experimento.
 */
public record RequestExperimentVeoVideoRequest(
        @NotNull ExperimentVideoSlot slot,
        @NotBlank String title,
        @NotBlank String objective,
        @NotBlank String primaryMetric,
        String personaName,
        String personaStyle,
        String voiceStyle,
        String language,
        Integer targetDurationSeconds,
        @NotBlank String scriptText,
        String hookText,
        String ctaText,
        String captionText,
        String providerName,
        SalesVideoExecutionMode executionMode,
        @NotBlank String requestedBy,
        boolean requiredForRelease
) { }
