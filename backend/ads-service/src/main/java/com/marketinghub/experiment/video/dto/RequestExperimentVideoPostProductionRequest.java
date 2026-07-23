package com.marketinghub.experiment.video.dto;

import com.marketinghub.salesvideo.SalesVideoExecutionMode;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitação administrativa para finalizar vídeos prontos com legenda, voz off e trilha.
 */
public record RequestExperimentVideoPostProductionRequest(
        @NotBlank String requestedBy,
        SalesVideoExecutionMode executionMode,
        String voiceOverScript,
        String captionText,
        String soundtrackStyle,
        String outputVariant,
        Boolean createShortDerivatives
) { }
