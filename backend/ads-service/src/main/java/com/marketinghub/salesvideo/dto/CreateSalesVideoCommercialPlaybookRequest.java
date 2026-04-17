package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSalesVideoCommercialPlaybookRequest {
    @NotBlank
    private String nicheKey;

    @NotBlank
    private String variantKey;

    @NotBlank
    private String objectionText;

    @NotBlank
    private String ctaText;

    private Boolean active;
    private String createdBy;
}
