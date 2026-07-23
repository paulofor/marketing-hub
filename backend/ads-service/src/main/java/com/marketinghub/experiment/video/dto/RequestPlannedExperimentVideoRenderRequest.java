package com.marketinghub.experiment.video.dto;

import com.marketinghub.salesvideo.SalesVideoExecutionMode;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitação administrativa para renderizar vídeos planejados já vinculados ao experimento.
 */
public record RequestPlannedExperimentVideoRenderRequest(
        @NotBlank String requestedBy,
        SalesVideoExecutionMode executionMode,
        String personaName,
        String personaStyle,
        String voiceStyle,
        String language,
        Boolean requiredForRelease
) { }
