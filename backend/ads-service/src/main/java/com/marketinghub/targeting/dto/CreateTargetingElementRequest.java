package com.marketinghub.targeting.dto;

import com.marketinghub.targeting.TargetingElementSource;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload para criação manual ou via worker de elementos de segmentação.
 */
@Data
public class CreateTargetingElementRequest {
    private Long marketNicheId;
    private UUID hypothesisId;
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
    private BigDecimal confidence;
}
