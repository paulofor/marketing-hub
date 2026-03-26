package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoRetryReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload para reprocessar um job.
 */
@Data
public class RetrySalesVideoJobRequest {
    @NotBlank
    private String requestedBy;

    @NotNull
    private SalesVideoRetryReason reason;

    private String notes;
}
