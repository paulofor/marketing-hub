package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Solicitação administrativa para gerar script.
 */
@Data
public class GenerateSalesVideoScriptRequest {
    @NotBlank
    private String requestedBy;

    private String providerName;
}
