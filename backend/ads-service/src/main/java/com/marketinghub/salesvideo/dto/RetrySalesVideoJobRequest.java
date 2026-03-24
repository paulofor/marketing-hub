package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Payload para reprocessar um job.
 */
@Data
public class RetrySalesVideoJobRequest {
    @NotBlank
    private String requestedBy;
}
