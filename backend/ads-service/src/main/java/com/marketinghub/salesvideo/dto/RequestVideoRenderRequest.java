package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Solicitação para criar um job de renderização.
 */
@Data
public class RequestVideoRenderRequest {
    @NotBlank
    private String requestedBy;

    private SalesVideoProviderFamily providerFamily;
    private String providerName;
}
