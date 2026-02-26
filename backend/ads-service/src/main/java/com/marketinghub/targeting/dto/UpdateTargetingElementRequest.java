package com.marketinghub.targeting.dto;

import com.marketinghub.targeting.TargetingElementSource;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Payload para atualização/revisão dos elementos de segmentação.
 */
@Data
public class UpdateTargetingElementRequest {
    private TargetingElementType type;
    private String term;
    private String description;
    private String prompt;
    private String model;
    private TargetingElementSource source;
    private TargetingElementStatus status;
    private String notes;
    private String lastReviewedBy;
    private String metaId;
    private String metaKey;
    private Long metaAudienceSizeLowerBound;
    private Long metaAudienceSizeUpperBound;
    private BigDecimal confidence;
}
