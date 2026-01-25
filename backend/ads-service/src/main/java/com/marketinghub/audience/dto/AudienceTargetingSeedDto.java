package com.marketinghub.audience.dto;

import com.marketinghub.audience.TargetingSeedStatus;
import com.marketinghub.audience.TargetingSeedType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class AudienceTargetingSeedDto {
    private Long id;
    private TargetingSeedType type;
    private String value;
    private String metaId;
    private String key;
    private BigDecimal confidence;
    private TargetingSeedStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
