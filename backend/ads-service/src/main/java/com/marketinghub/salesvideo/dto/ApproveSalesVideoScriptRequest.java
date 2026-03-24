package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Payload para aprovar (ou subir manualmente) um script.
 */
@Data
public class ApproveSalesVideoScriptRequest {
    @NotBlank
    private String scriptText;

    private String hookText;
    private String ctaText;
    private String captionText;

    @NotBlank
    private String approvedBy;
}
