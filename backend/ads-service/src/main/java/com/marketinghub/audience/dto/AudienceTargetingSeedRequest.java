package com.marketinghub.audience.dto;

import com.marketinghub.audience.TargetingSeedStatus;
import com.marketinghub.audience.TargetingSeedType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AudienceTargetingSeedRequest {
    private TargetingSeedType type;
    private String value;
    private String metaId;
    private String key;
    private BigDecimal confidence;
    private TargetingSeedStatus status;
}
