package com.marketinghub.salesvideo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SalesVideoCommercialPlaybookDto {
    private Long id;
    private Long profileId;
    private String tenantId;
    private String nicheKey;
    private String variantKey;
    private String objectionText;
    private String ctaText;
    private boolean active;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
