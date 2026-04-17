package com.marketinghub.salesvideo.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

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
