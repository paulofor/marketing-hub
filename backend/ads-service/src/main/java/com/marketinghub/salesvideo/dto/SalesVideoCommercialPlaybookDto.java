package com.marketinghub.salesvideo.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

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
