package com.marketinghub.salesvideo.dto;

import com.marketinghub.salesvideo.SalesVideoConversionEventType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class SalesVideoConversionEventDto {
    private Long id;
    private Long profileId;
    private Long jobId;
    private Long scriptId;
    private String tenantId;
    private SalesVideoConversionEventType eventType;
    private BigDecimal eventValue;
    private String currencyCode;
    private String source;
    private Instant occurredAt;
    private String metadataJson;
    private Instant createdAt;
}
