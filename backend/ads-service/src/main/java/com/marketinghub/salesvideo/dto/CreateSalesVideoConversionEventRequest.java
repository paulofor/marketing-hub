package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoConversionEventType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Data
public class CreateSalesVideoConversionEventRequest {
  private Long jobId;
  private Long scriptId;

  @NotNull private SalesVideoConversionEventType eventType;

  private BigDecimal eventValue;
  private String currencyCode;
  private String source;
  private Instant occurredAt;
  private String metadataJson;
}
