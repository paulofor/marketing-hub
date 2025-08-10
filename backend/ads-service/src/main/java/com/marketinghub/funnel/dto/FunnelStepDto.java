package com.marketinghub.funnel.dto;

import com.marketinghub.funnel.ActionType;
import com.marketinghub.funnel.StimulusType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

/**
 * DTO representing a step within a funnel.
 */
@Data
public class FunnelStepDto {
    private UUID id;
    private Integer orderIdx;
    private StimulusType stimulusType;
    private String channel;
    private String templateId;
    private String note;
    private ActionType expectedAction;
    private Integer scoreInc;
    private BigDecimal revenueTarget;
    private Boolean isActive;
}
