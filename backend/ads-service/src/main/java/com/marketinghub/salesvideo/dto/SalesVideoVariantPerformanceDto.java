package com.marketinghub.salesvideo.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SalesVideoVariantPerformanceDto {
  private Long scriptId;
  private String providerName;
  private String variantKey;
  private long events;
  private long leads;
  private long purchases;
  private BigDecimal revenue;
}
