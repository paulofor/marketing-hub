package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Solicitação para finalizar um render bruto com áudio, legenda e trilha.
 */
@Data
public class RequestSalesVideoPostProductionRequest {
    @NotBlank
    private String requestedBy;

    private String sourceVideoUrl;

    @NotBlank
    private String voiceOverScript;

    @NotBlank
    private String captionText;
}
